package com.example.userauth.audit.aspect;

import com.example.userauth.audit.annotation.Audited;
import com.example.userauth.audit.entity.AuditEvent;
import com.example.userauth.audit.service.AuditEventService;
import com.example.userauth.audit.config.AuditingProperties;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.slf4j.MDC;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Aspect
@Component
@Order(1)
public class AuditedAspect {
    private final AuditEventService auditEventService;
    private final AuditingProperties auditingProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuditedAspect(AuditEventService auditEventService, AuditingProperties auditingProperties) {
        this.auditEventService = auditEventService;
        this.auditingProperties = auditingProperties;
    }

    @Around("@annotation(com.example.userauth.audit.annotation.Audited)")
    public Object auditMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        // Check if auditing is enabled
        if (!auditingProperties.isEnabled()) {
            return joinPoint.proceed();
        }
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Audited audited = signature.getMethod().getAnnotation(Audited.class);
        String action = audited.action();
        String resourceType = audited.resourceType();
        // Get traceId from MDC or generate if missing
        String traceId = MDC.get("X-Request-ID");
        if (traceId == null) {
            traceId = UUID.randomUUID().toString();
        }

        // Get authenticated user
        String userId = "system";
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getName() != null) {
            userId = authentication.getName();
        }

        // Get HTTP request info
        String clientIp = "unknown";
        String userAgent = "unknown";
        String referer = "unknown";
        String clientSource = "unknown";
        String requestedWith = "unknown";
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
            HttpServletRequest request = servletRequestAttributes.getRequest();
            clientIp = request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
            userAgent = request.getHeader("User-Agent") != null ? request.getHeader("User-Agent") : "unknown";
            referer = request.getHeader("Referer") != null ? request.getHeader("Referer") : "unknown";
            clientSource = request.getHeader("X-Client-Source") != null ? request.getHeader("X-Client-Source") : "unknown";
            requestedWith = request.getHeader("X-Requested-With") != null ? request.getHeader("X-Requested-With") : "unknown";
        }

        String outcome = "SUCCESS";
        String details = null;
        String oldValues = null;
        String responseHash = null;
        Object result = null;

        // Serialize method arguments as oldValues (before)
        try {
            oldValues = objectMapper.writeValueAsString(joinPoint.getArgs());
        } catch (Exception e) {
            oldValues = "[unserializable arguments]";
        }
        try {
            result = joinPoint.proceed();
            // Compute hash of return value as responseHash
            try {
                String responseString = objectMapper.writeValueAsString(result);
                responseHash = sha256Hex(responseString);
            } catch (Exception e) {
                responseHash = "[unserializable result]";
            }
            outcome = "SUCCESS";
            return result;
        } catch (Throwable ex) {
            outcome = "FAILURE";
            throw ex;
        } finally {
            AuditEvent event = new AuditEvent();
            event.setAction(action);
            event.setResourceType(resourceType);
            event.setTraceId(traceId);
            event.setUserId(userId);
            event.setOutcome(outcome);
            event.setDetails(details);
            event.setOldValues(oldValues);
            // Do not set newValues
            event.setResponseHash(responseHash);
            event.setClientIp(clientIp);
            event.setUserAgent(userAgent);
            event.setReferer(referer);
            event.setClientSource(clientSource);
            event.setRequestedWith(requestedWith);
            auditEventService.recordEvent(event, null);
        }
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}
