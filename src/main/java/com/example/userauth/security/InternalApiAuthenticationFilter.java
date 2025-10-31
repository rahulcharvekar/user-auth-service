package com.example.userauth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authenticates internal service-to-service requests that rely on the shared internal API key.
 * This ensures internal endpoints are no longer publicly accessible even when they bypass JWT.
 */
@Component
public class InternalApiAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(InternalApiAuthenticationFilter.class);
    private static final String[] INTERNAL_MATCHERS = {"/internal/auth/**", "/internal/authz/**"};
    private static final String INTERNAL_ROLE = "ROLE_INTERNAL_API_CALLER";

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final String expectedApiKey;
    private final String apiKeyHeader;

    public InternalApiAuthenticationFilter(
            @Value("${auth.internal.api-key:}") String internalApiKey,
            @Value("${auth.internal.api-key-header:X-Internal-Api-Key}") String apiKeyHeader) {
        this.expectedApiKey = internalApiKey;
        this.apiKeyHeader = StringUtils.hasText(apiKeyHeader) ? apiKeyHeader : "X-Internal-Api-Key";
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        for (String pattern : INTERNAL_MATCHERS) {
            if (pathMatcher.match(pattern, path)) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!StringUtils.hasText(expectedApiKey)) {
            logger.error("Internal API key is not configured; rejecting access to {}", request.getRequestURI());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Internal API access is not configured");
            return;
        }

        String providedKey = request.getHeader(apiKeyHeader);
        if (!expectedApiKey.equals(providedKey)) {
            logger.warn("Rejected internal request to {} due to missing/invalid API key", request.getRequestURI());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid internal API key");
            return;
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "internal-api", null, List.of(new SimpleGrantedAuthority(INTERNAL_ROLE)));
        authentication.setDetails(request);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}
