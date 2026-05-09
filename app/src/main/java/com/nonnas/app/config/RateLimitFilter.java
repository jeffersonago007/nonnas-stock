package com.nonnas.app.config;

import com.nonnas.web.RateLimitExceededException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Rate limiting in-memory por IP — 100 req/min por padrão. Em T17/produção
 * distribuída este limiter ganha um back-end Redis para lidar com vários
 * pods atrás do load balancer.
 *
 * <p>Quando o bucket esgota, lança {@link RateLimitExceededException} que o
 * {@code GlobalExceptionHandler} traduz em {@code 429 Too Many Requests}
 * com {@code Retry-After}.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final Bandwidth limit;
    private final boolean enabled;

    public RateLimitFilter(@Value("${nonnas.rate-limit.requests-per-minute:100}") int requestsPerMinute,
                           @Value("${nonnas.rate-limit.enabled:true}") boolean enabled) {
        this.limit = Bandwidth.builder()
                .capacity(requestsPerMinute)
                .refillIntervally(requestsPerMinute, Duration.ofMinutes(1))
                .build();
        this.enabled = enabled;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        if (!enabled || !request.getRequestURI().startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }
        Bucket bucket = buckets.computeIfAbsent(clientIp(request),
                ip -> Bucket.builder().addLimit(limit).build());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            chain.doFilter(request, response);
            return;
        }
        long retryAfter = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());
        log.warn("Rate limit excedido para IP {} — Retry-After {}s", clientIp(request), retryAfter);
        throw new RateLimitExceededException(Math.max(retryAfter, 1));
    }

    private static String clientIp(HttpServletRequest request) {
        String fwd = request.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) {
            int comma = fwd.indexOf(',');
            return (comma > 0 ? fwd.substring(0, comma) : fwd).trim();
        }
        return request.getRemoteAddr();
    }
}
