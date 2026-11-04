package com.fu.example.starter.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fu.example.starter.model.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 用户 Mapper 接口
 * </p>
 *
 * @author FU
 * @since 2025-11-11
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

}
