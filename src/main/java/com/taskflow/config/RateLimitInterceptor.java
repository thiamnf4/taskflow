package com.taskflow.config;

import com.taskflow.util.TenantContext;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    
    @Value("${rate-limit.capacity}")
    private int capacity;
    
    @Value("${rate-limit.refill-tokens}")
    private int refillTokens;
    
    @Value("${rate-limit.refill-duration-minutes}")
    private int refillDurationMinutes;
    
    private final Map<Long, Bucket> buckets = new ConcurrentHashMap<>();
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, 
                            Object handler) throws Exception {
        
        Long tenantId = TenantContext.getCurrentTenant();
        
        // Skip rate limiting for auth endpoints
        if (request.getRequestURI().contains("/api/v1/auth/")) {
            return true;
        }
        
        if (tenantId == null) {
            return true;
        }
        
        Bucket bucket = buckets.computeIfAbsent(tenantId, this::createNewBucket);
        
        if (bucket.tryConsume(1)) {
            return true;
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("Rate limit exceeded. Please try again later.");
            return false;
        }
    }
    
    private Bucket createNewBucket(Long tenantId) {
        Bandwidth limit = Bandwidth.classic(capacity, 
                Refill.intervally(refillTokens, Duration.ofMinutes(refillDurationMinutes)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
