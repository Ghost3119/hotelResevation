package com.hotelmanager.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AuthRateLimitFilter.class);

    private final Map<String, Deque<Long>> attemptsByKey = new ConcurrentHashMap<>();
    private final AtomicLong requestCounter = new AtomicLong();
    private final Clock clock;
    private final ClientIpResolver clientIpResolver;

    @Value("${app.security.auth-rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${app.security.auth-rate-limit.max-attempts:30}")
    private int maxAttempts;

    @Value("${app.security.auth-rate-limit.window-seconds:60}")
    private long windowSeconds;

    @Value("${app.security.auth-rate-limit.max-tracked-keys:10000}")
    private int maxTrackedKeys;

    @Autowired
    public AuthRateLimitFilter(ClientIpResolver clientIpResolver) {
        this(clientIpResolver, Clock.systemUTC());
    }

    AuthRateLimitFilter(ClientIpResolver clientIpResolver, Clock clock) {
        this.clientIpResolver = clientIpResolver;
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!enabled || !isAuthWrite(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = clientIpResolver.resolve(request);
        String key = request.getRequestURI() + ":" + clientIp;
        long now = clock.millis();
        long cutoff = now - windowSeconds * 1000L;
        if ((requestCounter.incrementAndGet() & 255L) == 0L) {
            removeExpiredKeys(cutoff);
        }

        Deque<Long> attempts = attemptsByKey.get(key);
        if (attempts == null) {
            if (attemptsByKey.size() >= maxTrackedKeys) {
                removeExpiredKeys(cutoff);
                if (attemptsByKey.size() >= maxTrackedKeys) {
                    log.warn("AUTH_RATE_LIMIT_CAPACITY path={} trackedKeys={}",
                            request.getRequestURI(), attemptsByKey.size());
                    reject(response);
                    return;
                }
            }
            Deque<Long> newAttempts = new ArrayDeque<>();
            Deque<Long> existing = attemptsByKey.putIfAbsent(key, newAttempts);
            attempts = existing != null ? existing : newAttempts;
        }

        synchronized (attempts) {
            while (!attempts.isEmpty() && attempts.peekFirst() < cutoff) {
                attempts.removeFirst();
            }
            if (attempts.size() >= maxAttempts) {
                log.warn("AUTH_RATE_LIMIT method={} path={} ip={} attempts={} windowSeconds={}",
                        request.getMethod(), request.getRequestURI(), clientIp, attempts.size(), windowSeconds);
                reject(response);
                return;
            }
            attempts.addLast(now);
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAuthWrite(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String path = request.getRequestURI();
        return "/api/auth/login".equals(path) || "/api/auth/refresh".equals(path);
    }

    private void removeExpiredKeys(long cutoff) {
        attemptsByKey.entrySet().removeIf(entry -> {
            Deque<Long> values = entry.getValue();
            synchronized (values) {
                while (!values.isEmpty() && values.peekFirst() < cutoff) {
                    values.removeFirst();
                }
                return values.isEmpty();
            }
        });
    }

    private void reject(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Too many authentication attempts\"}");
    }
}
