package com.toghrakx.backend.filter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.toghrakx.backend.exception.ProblemDetailWriter;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

/**
 * Limits how often one client may call public, easily abused endpoints, using one Bucket4j
 * token bucket per client IP and endpoint. Buckets live in a Caffeine cache that forgets idle
 * clients by itself, so memory stays bounded without any manual cleanup.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    // Requests allowed per minute, per client IP
    private static final Map<String, Integer> LIMITS_PER_MINUTE = Map.of(
            "/api/search", 30,
            "/api/posts/popular", 30,
            "/api/auth/login", 10);

    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(5))
            .maximumSize(100_000)
            .build();

    private final ProblemDetailWriter problemDetailWriter;

    public RateLimitFilter(ProblemDetailWriter problemDetailWriter) {
        this.problemDetailWriter = problemDetailWriter;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !LIMITS_PER_MINUTE.containsKey(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        // Never read X-Forwarded-For directly: clients can send any value in it.
        // With server.forward-headers-strategy=native, Tomcat sets getRemoteAddr() to the real
        // client IP, taking it from X-Forwarded-For only when the request came through a trusted proxy.
        String key = request.getRemoteAddr() + ":" + path;
        Bucket bucket = buckets.get(key, k -> newBucket(LIMITS_PER_MINUTE.get(path)));

        if (!bucket.tryConsume(1)) {
            problemDetailWriter.write(request, response, HttpStatus.TOO_MANY_REQUESTS,
                    "Too many requests. Please wait a minute and try again.");
            return;
        }
        filterChain.doFilter(request, response);
    }

    // Allows a burst of `perMinute` requests, then refills gradually at `perMinute` per minute.
    private static Bucket newBucket(int perMinute) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(perMinute).refillGreedy(perMinute, Duration.ofMinutes(1)).build())
                .build();
    }
}
