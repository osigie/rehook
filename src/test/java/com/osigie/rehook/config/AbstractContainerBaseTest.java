package com.osigie.rehook.config;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;

@ActiveProfiles("test")
@Testcontainers
public abstract class AbstractContainerBaseTest {

    static final boolean DOCKER_AVAILABLE = isDockerAvailable();
    static final PostgreSQLContainer MY_POSTGRES_CONTAINER;
    static final RedisContainer MY_REDIS_CONTAINER;

    private static boolean isDockerAvailable() {
        try {
            Process p = Runtime.getRuntime().exec("docker ps");
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    static {
        if (DOCKER_AVAILABLE) {
            MY_POSTGRES_CONTAINER = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");

            MY_REDIS_CONTAINER = new RedisContainer(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379)
                    .withReuse(true);

            MY_POSTGRES_CONTAINER.start();
            MY_REDIS_CONTAINER.start();
        } else {
            MY_POSTGRES_CONTAINER = null;
            MY_REDIS_CONTAINER = null;
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (DOCKER_AVAILABLE) {
            registry.add("spring.datasource.url", MY_POSTGRES_CONTAINER::getJdbcUrl);
            registry.add("spring.datasource.username", MY_POSTGRES_CONTAINER::getUsername);
            registry.add("spring.datasource.password", MY_POSTGRES_CONTAINER::getPassword);
            registry.add("spring.flyway.url", MY_POSTGRES_CONTAINER::getJdbcUrl);
            registry.add("spring.flyway.user", MY_POSTGRES_CONTAINER::getUsername);
            registry.add("spring.flyway.password", MY_POSTGRES_CONTAINER::getPassword);

            registry.add("spring.data.redis.host", MY_REDIS_CONTAINER::getHost);
            registry.add("spring.data.redis.port", () -> MY_REDIS_CONTAINER.getMappedPort(6379).toString());
            registry.add("spring.data.redis.database", () -> "1");
        } else {
            registry.add("spring.datasource.url", () -> "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
            registry.add("spring.datasource.username", () -> "sa");
            registry.add("spring.datasource.password", () -> "");
            registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");

            registry.add("spring.flyway.enabled", () -> "false");

            registry.add("spring.data.redis.host", () -> "localhost");
            registry.add("spring.data.redis.port", () -> "6379");
            registry.add("spring.data.redis.database", () -> "1");

            registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        }
    }

    @BeforeEach
    void setUp() throws IOException, InterruptedException {
        if (DOCKER_AVAILABLE && MY_REDIS_CONTAINER != null) {
            MY_REDIS_CONTAINER.execInContainer("redis-cli", "FLUSHALL");
        }
    }

}
