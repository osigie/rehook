package com.osigie.rehook.filter;

import com.osigie.rehook.configuration.rate_limiting.RateLimiterConfig;
import com.osigie.rehook.exception.RateLimitException;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class RateLimitFilter implements Filter {

    private final ProxyManager<String> proxyManager;
    private final HandlerExceptionResolver resolver;
    private final RateLimiterConfig rateLimiterConfig;

    public RateLimitFilter(ProxyManager<String> proxyManager, @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver, RateLimiterConfig rateLimiterConfig) {
        this.proxyManager = proxyManager;
        this.resolver = resolver;
        this.rateLimiterConfig = rateLimiterConfig;
    }

    @Override
    public void doFilter(
            ServletRequest servletRequest,
            ServletResponse servletResponse,
            FilterChain filterChain
    ) {


        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;

        String path = httpRequest.getRequestURI();

        try {
            if (path.startsWith("/ingest/")) {
                String ingestionId = path.substring("/ingest/".length());
                consume("rate-limit:ingest:hook:" + ingestionId, rateLimiterConfig.bucketConfiguration(50L, 50L, Duration.ofMinutes(1)));
            }

            String ip = getClientIp(httpRequest);
            consume("rate-limit:ingest:ip:" + ip, rateLimiterConfig.bucketConfiguration(200L, 200L, Duration.ofMinutes(1)));

            filterChain.doFilter(servletRequest, servletResponse);

        } catch (Exception ex) {
            resolver.resolveException(httpRequest, httpResponse, null, ex);
        }
    }


    private void consume(String key, Supplier<BucketConfiguration> config) {
        Bucket bucket = proxyManager.builder().build(key, config);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (!probe.isConsumed()) {
            long retryAfterSeconds = Math.max(1, TimeUnit
                    .NANOSECONDS
                    .toSeconds(probe.getNanosToWaitForRefill()));

            throw new RateLimitException("Rate limit exceeded", retryAfterSeconds);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");

        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}
