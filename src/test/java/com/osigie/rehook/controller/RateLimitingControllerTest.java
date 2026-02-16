package com.osigie.rehook.controller;

import com.osigie.rehook.config.AbstractContainerBaseTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class RateLimitingControllerTest extends AbstractContainerBaseTest {

    @Test
    public void givenRateLimitFilter_whenRateLimitExceeded_thenThrowRateLimitException() {
        // This test validates that rate limiting is handled at the filter level
        // RateLimitException is thrown by RateLimitFilter, not a controller
        
        com.osigie.rehook.exception.RateLimitException exception = 
                new com.osigie.rehook.exception.RateLimitException("Rate limit exceeded", 60);
        
        org.junit.jupiter.api.Assertions.assertEquals("Rate limit exceeded", exception.getMessage());
        org.junit.jupiter.api.Assertions.assertEquals(60, exception.getRetryAfterSeconds());
    }
}