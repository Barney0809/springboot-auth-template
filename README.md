# Spring Boot 登录注册模板

一个可以直接拿来改的起步模板：Java 25 + Spring Boot 3.5.7，内置**用户名密码**与**手机号验证码**两条登录链路，配套 Redis 登录态、分布式锁、统一响应与全局异常、Swagger/Knife4j 文档。

不是框架，是一份摆好的骨架——把 `com.fu.example.starter` 换成你自己的包名就能开工。

## 包含什么

| 能力 | 说明 |
| --- | --- |
| 注册 / 登录 | 用户名 + 密码；手机号 + 短信验证码（验证码即注册，未注册自动建号） |
| 登录态 | 令牌存 Redis（24 小时），拦截器统一校验；`current` 取当前用户、`logout` 注销 |
| 短信 | 阿里云号码认证服务；Redis 计数做频率限制，验证码 5 分钟过期 |
| 并发控制 | Redis `SETNX` + Lua 脚本释放锁，处理注册/发码的并发与幂等 |
| 数据访问 | MyBatis-Plus + MySQL，雪花 ID，逻辑删除 |
| 工程化 | 统一响应体、全局异常、参数校验、请求日志切面、CORS、Swagger + Knife4j |
| 构建 | Maven Wrapper（自带，无需先装 Maven） |

## 技术栈

| 层 | 选型 |
| --- | --- |
| 运行时 | JDK 25、Spring Boot 3.5.7 |
| 数据 | MySQL 8、MyBatis-Plus 3.5.14 |
| 缓存 | Redis（Spring Data Redis） |
| 文档 | SpringDoc OpenAPI + Knife4j 4.5 |
| 工具 | Hutool、Lombok、fastjson2 |

## 快速开始

### 1. 建库

```bash
mysql -uroot -p < sql/schema.sql
```

脚本会创建 `starter_db` 与 `user` 表。

### 2. 起 Redis

任意本地 Redis 即可，默认 `localhost:6379`。有口令就用 `REDIS_PASSWORD` 传。

### 3. 配置

最少只需要这两项（其余都有本地默认值）：

```bash
export DB_PASSWORD=你的MySQL口令
export AUTH_PASSWORD_SALT=$(openssl rand -hex 16)   # 密码摘要用的盐，见下方「安全提示」
```

要用短信则再补阿里云凭据（走阿里云默认凭据链，不写进配置文件）：

```bash
export ALIBABA_CLOUD_ACCESS_KEY_ID=...
export ALIBABA_CLOUD_ACCESS_KEY_SECRET=...
```

### 4. 运行

```bash
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

- 接口根路径：`http://localhost:8080/api`
- Swagger UI：`http://localhost:8080/api/swagger-ui.html`
- Knife4j：`http://localhost:8080/api/doc.html`

## 接口一览

| 方法 | 路径 | 说明 | 需要登录 |
| --- | --- | --- | --- |
| POST | `/api/user/register` | 用户名密码注册 | 否 |
| POST | `/api/user/login` | 用户名密码登录 | 否 |
| POST | `/api/user/send` | 发送短信验证码 | 否 |
| POST | `/api/user/sms/login` | 手机号 + 验证码登录（未注册则自动注册） | 否 |
| GET | `/api/user/current` | 取当前登录用户 | 是 |
| POST | `/api/user/logout` | 注销 | 是 |

> 实际前缀是 `server.servlet.context-path` + `spring.mvc.servlet.path`（这里 `spring.mvc.servlet.path=/api`）。

## 把它当成模板用：替换包名

模板现在用的是 `com.fu.example.starter`。换成你自己的，按顺序做五步：

1. **改目录**

   ```
   src/main/java/com/fu/example/starter  →  src/main/java/<你的包路径>
   src/test/java/com/fu/example/starter  →  src/test/java/<你的包路径>
   ```

2. **全局替换包名**

   ```bash
   # Linux/macOS
   grep -rl 'com.fu.example.starter' src | xargs sed -i '' 's/com\.fu\.example\.starter/<你的包名>/g'
   ```

3. **改 `pom.xml`**：`groupId`、`artifactId`、`name`、`description`

4. **改 `application.properties`**：`spring.application.name`、`springdoc.group-configs[0].packages-to-scan`

5. **改启动类名**：`StarterApplication`（以及 `StarterApplicationTests`）

做完 `./mvnw -DskipTests compile` 确认能编译。

## 配置项

全部支持环境变量覆盖，`${VAR:默认值}` 里的就是该项的环境变量名。

| 配置 | 环境变量 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `spring.datasource.url` | `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `3306` / `starter_db` | 数据库 |
| `spring.datasource.username` | `DB_USERNAME` | `root` | |
| `spring.datasource.password` | `DB_PASSWORD` | 空 | **必须显式提供** |
| `spring.data.redis.host` | `REDIS_HOST` | `localhost` | |
| `spring.data.redis.port` | `REDIS_PORT` | `6379` | |
| `spring.data.redis.password` | `REDIS_PASSWORD` | 空 | |
| `auth.password-salt` | `AUTH_PASSWORD_SALT` | `CHANGE_ME_PASSWORD_SALT` | **上线前必须替换** |
| `aliyun.sms.sign-name` | `ALIYUN_SMS_SIGN_NAME` | 你的短信签名 | |
| `aliyun.sms.template-code` | `ALIYUN_SMS_TEMPLATE_CODE` | `100001` | |
| `aliyun.sms.endpoint` | `ALIYUN_SMS_ENDPOINT` | `dypnsapi.aliyuncs.com` | |
| 阿里云 AccessKey | `ALIBABA_CLOUD_ACCESS_KEY_ID` / `ALIBABA_CLOUD_ACCESS_KEY_SECRET` | 无 | 走阿里云默认凭据链 |

## 测试

```bash
./mvnw test
```

- `CommonResponseTest`：统一响应体的契约（`code == 0` 表示成功，前端依赖这个语义）
- `StarterApplicationTests`：启动上下文冒烟，需要 MySQL 与 Redis，默认跳过；要跑它：

```bash
STARTER_IT=true ./mvnw test -Dtest=StarterApplicationTests
```

## 安全提示（重要）

1. **`AUTH_PASSWORD_SALT` 必须换成你自己的随机值**，并且不要提交进仓库。盐一换，已入库的密码摘要全部失效。
2. **模板用的是 `MD5(盐 + 明文)`**，够一个练习项目用，但**不建议直接用于生产**。要升级成 BCrypt，只需：加依赖 `org.springframework.security:spring-security-crypto`，把 `UserServiceImpl#encryptPassword` 换成 `new BCryptPasswordEncoder().encode(...)`，登录处改成 `matches(...)`，并让旧用户首次登录时自动重写密码。
3. 数据库、Redis、短信凭据一律走环境变量，仓库里只留占位和空默认值。
4. 上线时务必收紧 `CorsConfig` 的允许来源，不要保留 `*`。

## 已知粗糙处（如实说明）

- `generator/CodeGenerator.java` 是本机用的代码生成工具，里面的数据库连接串需要自己改。
- `controller/TestController.java` 是联调用的示例接口，正式项目里可以删掉。
- `InterceptorConfig` 里有一批历史遗留的放行路径（如 `/api/v1/sys/login`），当前项目并未使用，可自行清理。
- 只有两个测试类，覆盖集中在响应封装；Service 层没有单测。

## 许可

[MIT](LICENSE)
