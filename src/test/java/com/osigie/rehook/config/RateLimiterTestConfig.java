package com.osigie.rehook.config;

import com.osigie.rehook.configuration.rate_limiting.RateLimiterConfig;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class RateLimiterTestConfig {

    @Bean
    @Primary
    RateLimiterConfig rateLimiterConfig() {
        return Mockito.mock(RateLimiterConfig.class);
    }
}
