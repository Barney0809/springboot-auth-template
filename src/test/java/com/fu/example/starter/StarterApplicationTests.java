package com.fu.example.starter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 启动上下文冒烟测试 —— 验证整个 Spring 容器能装配起来。
 *
 * <p>它会真实连接 MySQL 与 Redis，所以默认跳过：没有起依赖的人（或 CI）
 * 直接 {@code mvn test} 不会因为它变红。
 *
 * <p>要执行它：
 * <pre>
 * # 先准备好 MySQL（sql/schema.sql）与 Redis，配好环境变量，然后：
 * STARTER_IT=true mvn test -Dtest=StarterApplicationTests
 * </pre>
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "STARTER_IT", matches = "true")
class StarterApplicationTests {

    @Test
    void contextLoads() {
    }

}
