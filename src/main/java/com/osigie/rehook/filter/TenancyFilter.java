package com.osigie.rehook.filter;

import com.osigie.rehook.configuration.tenancy.TenantContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Component
@Slf4j
public class TenancyFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
//TODO: validate the tenant and also create a specail tenant retrieval for webhook endpoint through secret
        log.info("Request URL: {} ", httpRequest.getRequestURI());
        String tenantId = httpRequest.getHeader("x-tenant-id");

        TenantContext.set(TenantContext.builder().tenantId(tenantId).build());

        filterChain.doFilter(servletRequest, servletResponse); // Continue the filter chain

        log.info("Tenant Cleared {}", TenantContext.get().getTenantId());
        TenantContext.clear();
    }
}
