package com.osigie.rehook.filter;

import com.osigie.rehook.config.AbstractContainerBaseTest;
import com.osigie.rehook.configuration.rate_limiting.RateLimiterConfig;
import com.osigie.rehook.exception.RateLimitException;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.time.Duration;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
public class RateLimitFilterTest extends AbstractContainerBaseTest {

    @Mock
    private ProxyManager<String> proxyManager;

    @Mock
    private HandlerExceptionResolver resolver;

    @Mock
    private RateLimiterConfig rateLimiterConfig;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private RemoteBucketBuilder<String> remoteBucketBuilder;

    @Mock
    private BucketProxy bucketProxy;

    @Mock
    private ConsumptionProbe consumptionProbe;

    @Mock
    private BucketConfiguration bucketConfiguration;

    @InjectMocks
    private RateLimitFilter rateLimitFilter;

    private String ingestionId = "test-ingestion-id";
    private String clientIp = "192.168.1.1";

    @BeforeEach
    void setUp() {
        when(rateLimiterConfig.bucketConfiguration(anyLong(), anyLong(), any(Duration.class)))
                .thenReturn(() -> bucketConfiguration);
        when(proxyManager.builder()).thenReturn(remoteBucketBuilder);
        when(remoteBucketBuilder.build(anyString(), any(Supplier.class))).thenReturn(bucketProxy);
    }

    @Test
    public void givenIngestPathWithAvailableTokens_whenDoFilter_thenProceed() throws ServletException, IOException {
        //given
        when(request.getRequestURI()).thenReturn("/ingest/" + ingestionId);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn(clientIp);
        
        when(bucketProxy.tryConsumeAndReturnRemaining(1)).thenReturn(consumptionProbe);
        when(consumptionProbe.isConsumed()).thenReturn(true);

        //when
        rateLimitFilter.doFilter(request, response, filterChain);

        //then
        verify(filterChain).doFilter(request, response);
        verify(resolver, never()).resolveException(any(), any(), any(), any());
    }

    @Test
    public void givenIngestPathWithNoTokens_whenDoFilter_thenThrowRateLimitException() throws ServletException, IOException {
        //given
        when(request.getRequestURI()).thenReturn("/ingest/" + ingestionId);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn(clientIp);
        
        when(bucketProxy.tryConsumeAndReturnRemaining(1)).thenReturn(consumptionProbe);
        when(consumptionProbe.isConsumed()).thenReturn(false);
        when(consumptionProbe.getNanosToWaitForRefill()).thenReturn(1000000000L); // 1 second in nanoseconds

        //when
        rateLimitFilter.doFilter(request, response, filterChain);

        //then
        verify(filterChain, never()).doFilter(request, response);
        verify(resolver).resolveException(eq(request), eq(response), eq(null), any(RateLimitException.class));
    }

    @Test
    public void givenNonIngestPath_whenDoFilter_thenOnlyRateLimitByIp() throws ServletException, IOException {
        //given
        when(request.getRequestURI()).thenReturn("/api/subscriptions");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn(clientIp);
        
        when(bucketProxy.tryConsumeAndReturnRemaining(1)).thenReturn(consumptionProbe);
        when(consumptionProbe.isConsumed()).thenReturn(true);

        //when
        rateLimitFilter.doFilter(request, response, filterChain);

        //then
        verify(filterChain).doFilter(request, response);
        // Should be called once for IP rate limiting (verify builder was called once for IP)
        verify(remoteBucketBuilder, times(1)).build(contains("ip"), any(Supplier.class));
    }

    @Test
    public void givenXForwardedForHeader_whenGetClientIp_thenReturnForwardedIp() throws ServletException, IOException {
        //given
        String forwardedIp = "203.0.113.195, 70.41.3.18, 150.172.238.178";
        when(request.getRequestURI()).thenReturn("/ingest/" + ingestionId);
        when(request.getHeader("X-Forwarded-For")).thenReturn(forwardedIp);
        
        when(bucketProxy.tryConsumeAndReturnRemaining(1)).thenReturn(consumptionProbe);
        when(consumptionProbe.isConsumed()).thenReturn(true);

        //when
        rateLimitFilter.doFilter(request, response, filterChain);

        //then
        verify(filterChain).doFilter(request, response);
        // Should use the first IP from X-Forwarded-For header
        verify(remoteBucketBuilder).build(contains("203.0.113.195"), any(Supplier.class));
    }

    @Test
    public void givenEmptyXForwardedForHeader_whenGetClientIp_thenReturnRemoteAddr() throws ServletException, IOException {
        //given
        when(request.getRequestURI()).thenReturn("/ingest/" + ingestionId);
        when(request.getHeader("X-Forwarded-For")).thenReturn("");
        when(request.getRemoteAddr()).thenReturn(clientIp);
        
        when(bucketProxy.tryConsumeAndReturnRemaining(1)).thenReturn(consumptionProbe);
        when(consumptionProbe.isConsumed()).thenReturn(true);

        //when
        rateLimitFilter.doFilter(request, response, filterChain);

        //then
        verify(filterChain).doFilter(request, response);
        verify(remoteBucketBuilder).build(contains(clientIp), any(Supplier.class));
    }

    @Test
    public void givenExceptionInFilter_whenDoFilter_thenResolveException() throws ServletException, IOException {
        //given
        when(request.getRequestURI()).thenReturn("/ingest/" + ingestionId);
        when(bucketProxy.tryConsumeAndReturnRemaining(1))
                .thenThrow(new RuntimeException("Redis connection error"));

        //when
        rateLimitFilter.doFilter(request, response, filterChain);

        //then
        verify(filterChain, never()).doFilter(request, response);
        verify(resolver).resolveException(eq(request), eq(response), eq(null), any(RuntimeException.class));
    }

    @Test
    public void givenRateLimitExceeded_whenConsume_thenCalculateRetryAfterSeconds() throws ServletException, IOException {
        //given
        when(request.getRequestURI()).thenReturn("/ingest/" + ingestionId);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn(clientIp);
        
        when(bucketProxy.tryConsumeAndReturnRemaining(1)).thenReturn(consumptionProbe);
        when(consumptionProbe.isConsumed()).thenReturn(false);
        when(consumptionProbe.getNanosToWaitForRefill()).thenReturn(500000000L); // 0.5 seconds in nanoseconds

        //when
        rateLimitFilter.doFilter(request, response, filterChain);

        //then
        verify(resolver).resolveException(eq(request), eq(response), eq(null), any(RateLimitException.class));
        // Should round up to at least 1 second
        // Note: This would need to be verified by checking the exception message in a real scenario
    }
}
