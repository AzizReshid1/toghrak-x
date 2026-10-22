package com.toghrakx.backend.filter;

import com.toghrakx.backend.exception.ProblemDetailWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MS = 60_000;
    /** Sweep stale entries from the counters map every N processed requests. */
    private static final int SWEEP_EVERY = 500;

    private record Limit(int maxRequests) {
    }

    private static class Counter {
        AtomicInteger count = new AtomicInteger(0);
        volatile long windowStart = System.currentTimeMillis();
    }

    private final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<>();
    private final AtomicInteger sweepTick = new AtomicInteger(0);
    private final Map<String, Limit> limits = Map.of(
            "/api/search", new Limit(30),
            "/api/posts/popular", new Limit(30),
            "/api/auth/login", new Limit(10));

    private final ProblemDetailWriter problemDetailWriter;

    public RateLimitFilter(ProblemDetailWriter problemDetailWriter) {
        this.problemDetailWriter = problemDetailWriter;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        // Apply to public, potentially abused endpoints
        return !limits.containsKey(path);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String key = resolveKey(request);
        Counter counter = counters.computeIfAbsent(key, k -> new Counter());
        int max = limits.getOrDefault(request.getRequestURI(), new Limit(30)).maxRequests();
        long now = System.currentTimeMillis();
        synchronized (counter) {
            if (now - counter.windowStart > WINDOW_MS) {
                counter.count.set(0);
                counter.windowStart = now;
            }
            if (counter.count.incrementAndGet() > max) {
                problemDetailWriter.write(request, response, HttpStatus.TOO_MANY_REQUESTS,
                        "Too many requests. Please wait a minute and try again.");
                return;
            }
        }
        // Periodically evict entries whose window has already expired.
        // Without this, every unique IP adds a permanent entry — a slow memory leak.
        if (sweepTick.incrementAndGet() % SWEEP_EVERY == 0) {
            long cutoff = System.currentTimeMillis() - WINDOW_MS;
            counters.entrySet().removeIf(e -> e.getValue().windowStart < cutoff);
        }

        filterChain.doFilter(request, response);
    }

    private String resolveKey(HttpServletRequest request) {
        // Never read X-Forwarded-For directly: clients can send any value in it.
        // With server.forward-headers-strategy=native, Tomcat sets getRemoteAddr() to the real
        // client IP, taking it from X-Forwarded-For only when the request came through a trusted proxy.
        String ip = request.getRemoteAddr();
        String path = request.getRequestURI();
        return (ip == null ? "unknown" : ip) + ":" + path;
    }
}
