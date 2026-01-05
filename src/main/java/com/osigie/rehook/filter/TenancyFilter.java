package com.osigie.rehook.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osigie.rehook.configuration.tenancy.TenantContext;
import com.osigie.rehook.domain.model.Subscription;
import com.osigie.rehook.dto.response.ErrorResponseDto;
import com.osigie.rehook.exception.ResourceNotFoundException;
import com.osigie.rehook.repository.SubscriptionRepository;
import com.osigie.rehook.service.SubscriptionService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;


@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@Slf4j
public class TenancyFilter implements Filter {
    private final SubscriptionService subscriptionService;
    private final HandlerExceptionResolver resolver;

    public TenancyFilter(SubscriptionService subscriptionService, ObjectMapper objectMapper,
                         @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver
    ) {
        this.subscriptionService = subscriptionService;
        this.resolver = resolver;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) {
        HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        String path = httpRequest.getRequestURI();

        log.info("TenancyFilter path={}", path);

        try {
            if (path.startsWith("/ingest/")) {
                String ingestionId = path.substring("/ingest/".length());
                log.info("TenancyFilter ingestionId={}", ingestionId);
                Subscription subscription = subscriptionService.findByIngestionId(ingestionId);
                TenantContext.set(TenantContext.builder().tenantId(subscription.getTenant()).build());
                log.info("Tenant set from ingestion subscription: {}", subscription.getTenant());
            }
            filterChain.doFilter(servletRequest, servletResponse);
        } catch (Exception ex) {
            resolver.resolveException(httpRequest, httpResponse, null, ex);
        } finally {
            if (TenantContext.get() != null) {
                log.info("Tenant Cleared {}", TenantContext.get().getTenantId());
            }
            TenantContext.clear();
        }
    }
}
