package com.osigie.rehook.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osigie.rehook.configuration.tenancy.TenantContext;
import com.osigie.rehook.domain.model.Subscription;
import com.osigie.rehook.exception.ResourceNotFoundException;
import com.osigie.rehook.service.SubscriptionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TenancyFilterTest {

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private HandlerExceptionResolver resolver;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private TenancyFilter tenancyFilter;

    private Subscription subscription;
    private String ingestionId;

    @BeforeEach
    void setUp() throws Exception {
        ingestionId = "test-ingestion-id-123";

        subscription = new Subscription("Test Subscription");
        setFieldValue(subscription, "id", UUID.randomUUID());
        setFieldValue(subscription, "tenant", "test-tenant");
        setFieldValue(subscription, "ingestionId", ingestionId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private void setFieldValue(Object target, String fieldName, Object value) throws Exception {
        Field field = null;
        Class<?> clazz = target.getClass();
        
        // Search in the class hierarchy
        while (clazz != null) {
            try {
                field = clazz.getDeclaredField(fieldName);
                break;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        
        if (field == null) {
            throw new NoSuchFieldException("Field '" + fieldName + "' not found in class hierarchy of " + target.getClass().getName());
        }
        
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void givenIngestPath_whenDoFilter_thenSetTenantContext() throws Exception {
        // given
        String path = "/ingest/" + ingestionId;
        when(request.getRequestURI()).thenReturn(path);
        when(subscriptionService.findByIngestionId(ingestionId)).thenReturn(subscription);
        doNothing().when(filterChain).doFilter(request, response);

        // when
        tenancyFilter.doFilter(request, response, filterChain);

        // then
        verify(request, times(1)).getRequestURI();
        verify(subscriptionService, times(1)).findByIngestionId(ingestionId);
        verify(filterChain, times(1)).doFilter(request, response);
        
        // Verify tenant context is cleared after execution (in finally block)
        assertNull(TenantContext.get());
    }

    @Test
    void givenNonIngestPath_whenDoFilter_thenDoNotSetTenantContext() throws Exception {
        // given
        String path = "/api/subscriptions";
        when(request.getRequestURI()).thenReturn(path);
        doNothing().when(filterChain).doFilter(request, response);

        // when
        tenancyFilter.doFilter(request, response, filterChain);

        // then
        verify(request, times(1)).getRequestURI();
        verify(subscriptionService, never()).findByIngestionId(anyString());
        verify(filterChain, times(1)).doFilter(request, response);

        // Verify tenant context was not set
        assertNull(TenantContext.get());
    }

    @Test
    void givenRootPath_whenDoFilter_thenDoNotSetTenantContext() throws Exception {
        // given
        String path = "/";
        when(request.getRequestURI()).thenReturn(path);
        doNothing().when(filterChain).doFilter(request, response);

        // when
        tenancyFilter.doFilter(request, response, filterChain);

        // then
        verify(subscriptionService, never()).findByIngestionId(anyString());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void givenEmptyIngestionId_whenDoFilter_thenCallWithEmptyString() throws Exception {
        // given - path ends with just "/ingest/"
        String path = "/ingest/";
        when(request.getRequestURI()).thenReturn(path);
        when(subscriptionService.findByIngestionId("")).thenThrow(new ResourceNotFoundException("Subscription not found"));
        when(resolver.resolveException(any(), any(), any(), any())).thenReturn(null);
        // Note: filterChain.doFilter is never called when exception is thrown, so no stubbing needed

        // when
        tenancyFilter.doFilter(request, response, filterChain);

        // then - should call subscription service with empty string and handle the exception
        verify(subscriptionService, times(1)).findByIngestionId("");
        verify(resolver, times(1)).resolveException(any(), any(), any(), any());
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void givenNonExistentIngestionId_whenDoFilter_thenResolveException() throws Exception {
        // given
        String nonExistentId = "non-existent-id";
        String path = "/ingest/" + nonExistentId;
        ResourceNotFoundException exception = new ResourceNotFoundException("Subscription not found");

        when(request.getRequestURI()).thenReturn(path);
        when(subscriptionService.findByIngestionId(nonExistentId)).thenThrow(exception);
        when(resolver.resolveException(eq(request), eq(response), isNull(), eq(exception))).thenReturn(null);

        // when
        tenancyFilter.doFilter(request, response, filterChain);

        // then
        verify(subscriptionService, times(1)).findByIngestionId(nonExistentId);
        verify(resolver, times(1)).resolveException(eq(request), eq(response), isNull(), eq(exception));
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void givenAnyException_whenDoFilter_thenResolveException() throws Exception {
        // given
        String path = "/ingest/" + ingestionId;
        RuntimeException exception = new RuntimeException("Unexpected error");

        when(request.getRequestURI()).thenReturn(path);
        when(subscriptionService.findByIngestionId(ingestionId)).thenThrow(exception);
        when(resolver.resolveException(eq(request), eq(response), isNull(), eq(exception))).thenReturn(null);

        // when
        tenancyFilter.doFilter(request, response, filterChain);

        // then
        verify(subscriptionService, times(1)).findByIngestionId(ingestionId);
        verify(resolver, times(1)).resolveException(eq(request), eq(response), isNull(), eq(exception));
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void givenIngestPathWithNestedRoute_whenDoFilter_thenExtractFullPath() throws Exception {
        // given
        // Note: The filter extracts everything after "/ingest/" including nested paths
        String path = "/ingest/" + ingestionId + "/additional-path";
        when(request.getRequestURI()).thenReturn(path);
        when(subscriptionService.findByIngestionId(ingestionId + "/additional-path")).thenReturn(subscription);
        doNothing().when(filterChain).doFilter(request, response);

        // when
        tenancyFilter.doFilter(request, response, filterChain);

        // then
        verify(subscriptionService, times(1)).findByIngestionId(ingestionId + "/additional-path");
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void givenIngestPathWithQueryParams_whenDoFilter_thenExtractWithQueryString() throws Exception {
        // given
        // Note: The filter extracts everything after "/ingest/" including query params
        String pathWithQuery = "/ingest/" + ingestionId + "?param=value";
        when(request.getRequestURI()).thenReturn(pathWithQuery);
        when(subscriptionService.findByIngestionId(ingestionId + "?param=value")).thenReturn(subscription);
        doNothing().when(filterChain).doFilter(request, response);

        // when
        tenancyFilter.doFilter(request, response, filterChain);

        // then
        verify(subscriptionService, times(1)).findByIngestionId(ingestionId + "?param=value");
        verify(filterChain, times(1)).doFilter(request, response);
        
        // Verify tenant context is cleared after execution
        assertNull(TenantContext.get());
    }

    @Test
    void givenSuccessScenario_whenDoFilter_thenClearTenantContextAfter() throws Exception {
        // given
        String path = "/ingest/" + ingestionId;
        when(request.getRequestURI()).thenReturn(path);
        when(subscriptionService.findByIngestionId(ingestionId)).thenReturn(subscription);
        doNothing().when(filterChain).doFilter(request, response);

        // when
        tenancyFilter.doFilter(request, response, filterChain);

        // then
        // Tenant context should be cleared in finally block
        assertNull(TenantContext.get());
    }

    @Test
    void givenExceptionScenario_whenDoFilter_thenClearTenantContextAfter() throws Exception {
        // given
        String path = "/ingest/" + ingestionId;
        RuntimeException exception = new RuntimeException("Test exception");

        when(request.getRequestURI()).thenReturn(path);
        when(subscriptionService.findByIngestionId(ingestionId)).thenThrow(exception);
        when(resolver.resolveException(eq(request), eq(response), isNull(), eq(exception))).thenReturn(null);

        // when
        tenancyFilter.doFilter(request, response, filterChain);

        // then
        // Tenant context should be cleared even after exception
        assertNull(TenantContext.get());
    }

    @Test
    void givenApiHealthPath_whenDoFilter_thenDoNotSetTenantContext() throws Exception {
        // given
        String path = "/api/health";
        when(request.getRequestURI()).thenReturn(path);
        doNothing().when(filterChain).doFilter(request, response);

        // when
        tenancyFilter.doFilter(request, response, filterChain);

        // then
        verify(subscriptionService, never()).findByIngestionId(anyString());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void givenWebhooksPath_whenDoFilter_thenDoNotSetTenantContext() throws Exception {
        // given
        String path = "/api/v1/webhooks";
        when(request.getRequestURI()).thenReturn(path);
        doNothing().when(filterChain).doFilter(request, response);

        // when
        tenancyFilter.doFilter(request, response, filterChain);

        // then
        verify(subscriptionService, never()).findByIngestionId(anyString());
        verify(filterChain, times(1)).doFilter(request, response);
    }
}
