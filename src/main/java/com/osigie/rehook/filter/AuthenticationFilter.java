package com.osigie.rehook.filter;

import com.osigie.rehook.configuration.tenancy.TenantContext;
import com.osigie.rehook.service.JWTService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;


@Slf4j
public class AuthenticationFilter extends OncePerRequestFilter {
    private final JWTService jwtService;
    private final UserDetailsService userDetailsService;
    private final HandlerExceptionResolver resolver;

    public AuthenticationFilter(JWTService jwtService, UserDetailsService userDetailsService, @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.resolver = resolver;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }
            String token = authHeader.substring(7);
            String email = jwtService.extractEmail(token);
            String tenant = jwtService.extractTenant(token);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                if (jwtService.isValid(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                    );
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    TenantContext.set(TenantContext.builder().tenantId(tenant).build());
                    log.info("Tenant set from header or default: {}", tenant);
                }
            }
            filterChain.doFilter(request, response);
        } catch (Exception ex) {

            resolver.resolveException(request, response, null, ex);
//            if (resolver != null) {
//                resolver.resolveException(request, response, null, ex);
//            } else {
//                throw ex;
//            }
        } finally {
            TenantContext.clear();
        }

    }
}
