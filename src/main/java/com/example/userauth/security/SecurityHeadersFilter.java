package com.example.userauth.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Filter to add security headers to all responses
 */
@Component
public class SecurityHeadersFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (response instanceof HttpServletResponse httpResponse) {
            // Remove cache control headers to allow ETag-based caching
            httpResponse.setHeader("Cache-Control", null);
            httpResponse.setHeader("Pragma", null);
            httpResponse.setHeader("Expires", null);
            
            // Ensure XSS protection is enabled
            httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
        }
        chain.doFilter(request, response);
    }
}
