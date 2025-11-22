package com.osigie.rehook.filter;

import com.osigie.rehook.configuration.tenancy.TenantContext;
import com.osigie.rehook.domain.model.Subscription;
import com.osigie.rehook.repository.SubscriptionRepository;
import com.osigie.rehook.service.SubscriptionService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Component
@Slf4j
public class TenancyFilter implements Filter {
    private final SubscriptionService subscriptionService;

    public TenancyFilter(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
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
            } else {
                String tenantId = httpRequest.getHeader("x-tenant-id");
                if (tenantId == null || tenantId.isBlank()) {
                    tenantId = "default";
                }
                TenantContext.set(TenantContext.builder().tenantId(tenantId).build());
                log.info("Tenant set from header or default: {}", tenantId);
            }

            filterChain.doFilter(servletRequest, servletResponse);

        } finally {
            if (TenantContext.get() != null) {
                log.info("Tenant Cleared {}", TenantContext.get().getTenantId());
            }
            TenantContext.clear();
        }
    }
}
