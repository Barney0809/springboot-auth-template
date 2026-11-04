package com.fu.example.starter.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.generator.SnowflakeGenerator;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fu.example.starter.common.UserContextHolder;
import com.fu.example.starter.constant.LuaScriptConstant;
import com.fu.example.starter.constant.RoleConstant;
import com.fu.example.starter.constant.UserConstant;
import com.fu.example.starter.exception.BusinessException;
import com.fu.example.starter.exception.ExceptionCode;
import com.fu.example.starter.model.dto.LoginUserDto;
import com.fu.example.starter.model.entity.User;
import com.fu.example.starter.mapper.UserMapper;
import com.fu.example.starter.model.enums.LuaStatusEnum;
import com.fu.example.starter.model.request.LoginRequest;
import com.fu.example.starter.model.request.RegisterRequest;
import com.fu.example.starter.model.request.UserSmsRequest;
import com.fu.example.starter.service.SmsService;
import com.fu.example.starter.service.UserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fu.example.starter.utils.SnowUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 用户 服务实现类
 * </p>
 *
 * @author FU
 * @since 2025-11-11
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private SmsService smsService;

    /**
     * 密码摘要用的盐。
     *
     * <p>从配置读取（{@code auth.password-salt} / 环境变量 {@code AUTH_PASSWORD_SALT}），
     * 不再写死在代码里：写死等于把盐随模板一起发出去，所有人都能拿它建彩虹表。
     *
     * <p>默认值是占位符，**上线前必须替换**。注意盐一旦更换，已入库的密码摘要即全部失效，
     * 只能让用户重置密码。
     */
    @Value("${auth.password-salt:CHANGE_ME_PASSWORD_SALT}")
    private String passwordSalt;

    @Override
    public LoginUserDto login(LoginRequest loginRequest) {
        // 0. 校验
        if (loginRequest == null) {
            log.error("登录错误！loginRequest参数为空");
            throw new BusinessException(ExceptionCode.PARAMS_ERROR);
        }
        if (loginRequest.getUsername() == null || loginRequest.getPassword() == null) {
            log.error("登录错误！用户名或密码为空");
            throw new BusinessException(ExceptionCode.PARAMS_ERROR);
        }
        // 1. 加密
        String encryptPassword = encryptPassword(loginRequest.getPassword());
        // 2. 查询
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, loginRequest.getUsername())
                .eq(User::getPassword, encryptPassword);
        User user = this.getOne(queryWrapper);
        if (user == null) {
            log.error("登录错误！用户【{}】不存在", loginRequest.getUsername());
            throw new BusinessException(ExceptionCode.USERNAME_OR_PASSWORD_ERROR);
        }

        LoginUserDto loginUser = BeanUtil.copyProperties(user, LoginUserDto.class);

        // 4. 执行登录逻辑并返回
        return executeLoginByRedis(loginUser);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LoginUserDto smsLoginOrRegister(UserSmsRequest userSmsRequest) {
        // 根据一般逻辑，验证码登录的时候一般会“没有注册过的用户将自动注册”，所以要修改这个接口，使其既支持注册又是登录
        boolean needRegisterFlag = false;

        // 0. 校验 其中userSmsRequest.getUsername() 代表的是PhoneNum
        if (userSmsRequest == null) {
            log.error("短信登录或注册错误！userSmsRequest参数为空");
            throw new BusinessException(ExceptionCode.PARAMS_ERROR);
        }
        if (userSmsRequest.getUsername() == null || userSmsRequest.getCode() == null) {
            log.error("短信登录或注册错误！参数不全");
            throw new BusinessException(ExceptionCode.PARAMS_ERROR);
        }

        // 1. 验证码校验
        boolean verifyResult = false;
        verifyResult = smsService.verifySmsCode(userSmsRequest.getUsername(), userSmsRequest.getCode());
        if (!verifyResult) {
            log.error("短信登录或注册错误！验证码错误");
            throw new BusinessException(ExceptionCode.VALIDATION_ERROR, "验证码错误！");
        }
        log.info("验证码校验成功！手机号：{}", userSmsRequest.getUsername());

        // 2. 查询数据库 是否存在该用户
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, userSmsRequest.getUsername());
        // 需要加悲观锁
        queryWrapper.last("FOR UPDATE");
        User user;
        LoginUserDto loginUser = new LoginUserDto();
        try {
            user = this.getOne(queryWrapper);
        } catch (RuntimeException e) {
            throw new BusinessException(ExceptionCode.OPERATION_ERROR, "数据库系统错误！");
        }

        // 3. 分支判断
        boolean needInitUserFlag = false; // 是否需要初始化用户信息，即user != null,getIsDelete == true;

        if (user == null) {
            needRegisterFlag = true;
            user = new User();
        } else if (Boolean.TRUE.equals(user.getIsDelete())) {
            needRegisterFlag = true;
            needInitUserFlag = true;
        }
        if (needRegisterFlag) {
            // 4. 注册
            // 4-1. 使用分布式锁防止并发注册
            String lockKey = UserConstant.REGISTER_LOCK_KEY + userSmsRequest.getUsername();
            String lockValue = UUID.randomUUID().toString();
            try {
                // 获取分布式锁（可使用Redis实现）
                int expireSeconds = 10;
                if (!acquireRegisterLock(lockKey, lockValue, expireSeconds)) {
                    log.error("注册错误！获取分布式锁失败");
                    throw new BusinessException(ExceptionCode.OPERATION_ERROR, "系统繁忙，请稍后重试");
                } else {
                    log.info("获取分布式锁成功！锁的key：{}", lockKey);
                }

                // 执行注册逻辑
                // 4-2. 判断是否是注销过的用户，初始化账号信息，如果是注销过的就复用ID，否则就生成新的ID
                user.setUsername(userSmsRequest.getUsername());
                user.setPassword(encryptPassword(UserConstant.DEFAULT_RANDOM_PASSWORD));
                user.setRole(RoleConstant.DEFAULT_ROLE);
                user.setIsDelete(false);
                user.setAvatar(getRandomDefaultAvatar());
                user.setProfile("");
                user.setNickname(getRandomDefaultNickname());
                user.setCreateTime(LocalDateTime.now());
                user.setUpdateTime(LocalDateTime.now());
                // 4-3. 更新数据库
                if (!needInitUserFlag) {
                    // 新用户 insert
                    user.setId(SnowUtil.getSnowflakeId());
                    log.info("手机号【{}】未注册，开始注册", userSmsRequest.getUsername());
                    // 唯一约束检查
                    boolean insertResult = false;
                    try {
                        insertResult = this.save(user);
                    } catch (DuplicateKeyException e) {
                        // 捕获唯一约束违反异常
                        log.warn("手机号【{}】并发注册冲突", userSmsRequest.getUsername(), e);
                        throw new BusinessException(ExceptionCode.OPERATION_ERROR, "手机号已被注册，请直接登录");
                    } catch (DataAccessException e) {
                        log.error("注册错误！手机号【{}】插入数据库失败", userSmsRequest.getUsername(), e);
                        throw new BusinessException(ExceptionCode.ERROR, "数据库系统错误！");
                    }
                    if (!insertResult) {
                        log.error("注册错误！手机号【{}】插入数据库失败", userSmsRequest.getUsername());
                        throw new BusinessException(ExceptionCode.ERROR, "数据库系统错误！");
                    } else {
                        log.info("手机号【{}】注册成功！", userSmsRequest.getUsername());
                    }
                } else {
                    // 注销再注册的用户 update
                    boolean updateResult = this.updateById(user);
                    if (!updateResult) {
                        log.error("注册错误！手机号【{}】更新数据库失败", userSmsRequest.getUsername());
                        throw new BusinessException(ExceptionCode.ERROR, "数据库系统错误！");
                    } else {
                        log.info("手机号【{}】曾经注册过但是删除了，初始化账号信息成功！", userSmsRequest.getUsername());
                    }
                }
            } finally {
                // 释放锁
                releaseRegisterLock(lockKey, lockValue);
                loginUser = doSmsLogin(user);
            }
        }
        if (!needRegisterFlag) {
            // 5. 登录
            loginUser = doSmsLogin(user);
        }
        return loginUser;
    }

    private LoginUserDto doSmsLogin(User user) {
        log.info(user.toString());
        LoginUserDto loginUser = BeanUtil.copyProperties(user, LoginUserDto.class);
        log.info("手机号【{}】，准备登录！", user.getUsername());
        return executeLoginByRedis(loginUser);
    }

    private LoginUserDto executeLoginByRedis(LoginUserDto loginUser) {
        // redis登录态
        String token = UUID.randomUUID().toString();
        log.info("登录成功！用户【{}】登录成功", loginUser.getUsername());
        loginUser.setToken(token);
        // 将用户信息转为Map并存入Redis，设置过期时间
        Map<String, Object> userMap = BeanUtil.beanToMap(loginUser, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
        userMap.put("login_time", LocalDateTime.now().toString());
        String userHashKey = UserConstant.USER_LOGIN_KEY + loginUser.getUsername();
        String tokenKey = UserConstant.TOKEN_KEY + token;
        try {
            redisTemplate.opsForHash().putAll(userHashKey, userMap);
            redisTemplate.opsForValue().set(tokenKey, loginUser.getUsername(), UserConstant.USER_LOGIN_TOKEN_EXPIRE, TimeUnit.HOURS);
            redisTemplate.expire(userHashKey, UserConstant.USER_LOGIN_TOKEN_EXPIRE, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("登录时写入redis异常！", e);
            throw new BusinessException(ExceptionCode.ERROR, "系统错误");
        }
        return loginUser;
    }

    private String encryptPassword(String password) {
        Optional.ofNullable(password).orElseThrow(() -> new BusinessException(ExceptionCode.PARAMS_ERROR, "Password为空"));
        return DigestUtils.md5DigestAsHex((passwordSalt + password).getBytes());
    }

    private String getRandomDefaultAvatar() {
        // 随机选择一张头像
        int randomIndex = new Random().nextInt(UserConstant.RANDOM_AVATAR_COUNT);
        return UserConstant.RANDOM_AVATARS[randomIndex];
    }

    private String getRandomDefaultNickname() {
        // 随机选择一个昵称
        return UserConstant.DEFAULT_NICKNAME_PREFIX + RandomUtil.randomString(6);
    }

    private boolean acquireRegisterLock(String key, String value, int expireSeconds) {
        // 使用Redis SETNX 实现分布式锁
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, value, expireSeconds, TimeUnit.SECONDS);
        return result != null && result;
    }

    private void releaseRegisterLock(String key, String value) {
        // Lua脚本保证原子性删除锁
        try {
            long result = redisTemplate.execute(LuaScriptConstant.UNLOCK_REDIS_SCRIPT, Collections.singletonList(key), value);
            if (LuaStatusEnum.FAIL.getValue() == result) {
                // 锁释放失败，可能是锁已过期或被其他线程持有
                log.warn("Redis锁释放失败, key: {}, value: {}", key, value);
            } else if (LuaStatusEnum.SUCCESS.getValue() == result) {
                // 锁释放成功
                log.info("Redis锁释放成功, key: {}, value: {}", key, value);
            }
        } catch (Exception e) {
            log.error("Redis锁释放失败, key: {}, value: {}", key, value, e);
            // 锁释放失败不应影响主业务流程，仅记录日志即可
            // 如果确实需要通知调用方，应通过返回值而非抛出异常
        }
    }

    @Override
    public LoginUserDto getCurrentUser() {
        LoginUserDto loginUser = UserContextHolder.getLoginUser();
        if (loginUser == null) {
            throw new BusinessException(ExceptionCode.USER_NOT_LOGIN, "用户未登录");
        }
        return loginUser;
    }

    @Override
    public void logout() {
        // 1. 获取当前用户
        LoginUserDto loginUser = UserContextHolder.getLoginUser();
        if (loginUser == null) {
            log.warn("用户未登录，无需登出");
            return;
        }

        try {
            // 2. 清理 ThreadLocal（提前清理，确保即使Redis操作失败也能正确清理上下文）
            UserContextHolder.clear();

            // 3. 删除Redis中的用户信息
            redisTemplate.delete(UserConstant.USER_LOGIN_KEY + loginUser.getUsername());
            // 4. 删除Redis中的token
            redisTemplate.delete(UserConstant.TOKEN_KEY + loginUser.getToken());

            log.info("用户登出成功，用户名：{}", loginUser.getUsername());
        } catch (Exception e) {
            log.error("用户登出异常，用户名：{}", loginUser.getUsername(), e);
            throw new BusinessException(ExceptionCode.ERROR, "系统错误");
        }
    }

    /**
     * 注册
     *
     * @param registerRequest 注册请求
     * @return 注册用户ID
     */
    @Override
    @Transactional
    public Long register(RegisterRequest registerRequest) {
        // 1. 参数校验
        if (registerRequest == null) {
            log.error("注册参数错误，registerRequest为空");
            throw new BusinessException(ExceptionCode.PARAMS_ERROR, "参数错误");
        }
        if (registerRequest.getUsername() == null
                || registerRequest.getPassword() == null
                || registerRequest.getCheckPassword() == null) {
            log.error("注册参数错误，username或password为空");
            throw new BusinessException(ExceptionCode.PARAMS_ERROR, "参数错误");
        }
        if (!registerRequest.getPassword().equals(registerRequest.getCheckPassword())) {
            log.error("注册参数错误，两次输入密码不一致");
            throw new BusinessException(ExceptionCode.PARAMS_ERROR, "两次输入密码不一致");
        }
        // 2. 数据库校验
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, registerRequest.getUsername());
        queryWrapper.last("FOR UPDATE");
        User user;
        try {
            user = this.getOne(queryWrapper);
        } catch (Exception e) {
            log.error("注册时数据库查询异常！", e);
            throw new BusinessException(ExceptionCode.ERROR, "数据库系统错误");
        }
        if (user != null && Boolean.FALSE.equals(user.getIsDelete())) {
            log.warn("用户已注册，用户名：{}", registerRequest.getUsername());
            throw new BusinessException(ExceptionCode.OPERATION_ERROR, "用户已注册");
        }
        // 分支：2-1. 是否注册过
        boolean needRegisterFlag = false;
        String lockKey = UserConstant.REGISTER_LOCK_KEY + registerRequest.getUsername();
        String lockValue = UUID.randomUUID().toString();
        try {
            // 获取分布式锁（可使用Redis实现）
            int expireSeconds = 10;
            if (!acquireRegisterLock(lockKey, lockValue, expireSeconds)) {
                log.error("用户名注册错误！获取分布式锁失败");
                throw new BusinessException(ExceptionCode.OPERATION_ERROR, "系统繁忙，请稍后重试");
            } else {
                log.info("用户名注册获取分布式锁成功！锁的key：{}", lockKey);
            }

            if (user == null) {
                log.info("用户未注册，开始注册用户：{}", registerRequest.getUsername());
                user = new User();
                user.setId(SnowUtil.getSnowflakeId());
                needRegisterFlag = true;
            }
            // 分支：2-2. 是否注销再注册
            if (Boolean.TRUE.equals(user.getIsDelete())) {
                log.info("用户已注销，开始重新注册用户：{}", registerRequest.getUsername());
            }
            // 3. 密码加密
            String encryptPassword = encryptPassword(registerRequest.getPassword());
            // 4. 插入数据库
            user.setUsername(registerRequest.getUsername());
            user.setRole(RoleConstant.DEFAULT_ROLE);
            user.setAvatar(getRandomDefaultAvatar());
            user.setNickname(getRandomDefaultNickname());
            user.setPassword(encryptPassword);
            user.setProfile("");
            user.setIsDelete(false);
            user.setCreateTime(LocalDateTime.now());
            user.setUpdateTime(LocalDateTime.now());
            if (needRegisterFlag) {
                // 插入
                try {
                    this.save(user);
                } catch (DuplicateKeyException e) {
                    // 捕获唯一约束违反异常
                    log.warn("用户名【{}】并发注册冲突", registerRequest.getUsername(), e);
                    throw new BusinessException(ExceptionCode.OPERATION_ERROR, "用户名已被注册！");
                } catch (RuntimeException e) {
                    log.error("注册时数据库插入异常！", e);
                    throw new BusinessException(ExceptionCode.ERROR, "数据库系统错误");
                }
            } else {
                // 更新
                try {
                    this.updateById(user);
                } catch (Exception e) {
                    log.error("注册时数据库更新异常！", e);
                    throw new BusinessException(ExceptionCode.ERROR, "数据库系统错误");
                }
            }
        } finally {
            // 释放锁
            releaseRegisterLock(lockKey, lockValue);
        }
        // 5. 返回用户ID
        return user.getId();
    }


}
