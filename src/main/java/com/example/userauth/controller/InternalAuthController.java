package com.example.userauth.controller;

import com.example.userauth.dto.internal.TokenIntrospectionRequest;
import com.example.userauth.dto.internal.TokenIntrospectionResponse;
import com.example.userauth.service.TokenIntrospectionService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/auth")
public class InternalAuthController {

    private static final Logger log = LoggerFactory.getLogger(InternalAuthController.class);

    private final TokenIntrospectionService tokenIntrospectionService;
    private final String expectedApiKey;
    private final String apiKeyHeader;

    public InternalAuthController(TokenIntrospectionService tokenIntrospectionService,
                                  @Value("${shared-lib.security.introspection.api-key:}") String sharedApiKey,
                                  @Value("${shared-lib.security.introspection.api-key-header:X-Internal-Api-Key}") String sharedHeader,
                                  @Value("${auth.internal.api-key:}") String overrideApiKey,
                                  @Value("${auth.internal.api-key-header:}") String overrideHeader) {
        this.tokenIntrospectionService = tokenIntrospectionService;
        this.expectedApiKey = StringUtils.hasText(overrideApiKey) ? overrideApiKey : sharedApiKey;
        this.apiKeyHeader = StringUtils.hasText(overrideHeader) ? overrideHeader : sharedHeader;
    }

    @PostMapping("/introspect")
    public ResponseEntity<TokenIntrospectionResponse> introspect(
        @Valid @RequestBody TokenIntrospectionRequest request,
        HttpServletRequest servletRequest) {

        if (StringUtils.hasText(expectedApiKey)) {
            String providedKey = servletRequest.getHeader(apiKeyHeader);
            if (!expectedApiKey.equals(providedKey)) {
                log.warn("Rejected introspection call due to invalid API key");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }

        try {
            TokenIntrospectionResponse response = tokenIntrospectionService.introspect(request.getToken());
            return ResponseEntity.ok(response);
        } catch (JwtException ex) {
            log.debug("Introspection failed: {}", ex.getMessage());
            return ResponseEntity.ok(TokenIntrospectionResponse.inactive());
        }
    }
}
