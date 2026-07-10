package com.hotelmanager.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class SecurityMonitoringFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SecurityMonitoringFilter.class);
    private static final int MAX_BODY_BYTES_TO_SCAN = 4096;

    private static final List<Signal> SIGNALS = List.of(
            new Signal("SQLI", Pattern.compile("(union\\s+select|\\bor\\s+1\\s*=\\s*1\\b|sleep\\s*\\(|benchmark\\s*\\(|information_schema|/\\*)", Pattern.CASE_INSENSITIVE)),
            new Signal("XSS", Pattern.compile("(<script|javascript:|onerror\\s*=|onload\\s*=|<iframe|<svg)", Pattern.CASE_INSENSITIVE)),
            new Signal("PATH_TRAVERSAL", Pattern.compile("(\\.\\./|%2e%2e|etc/passwd|boot\\.ini)", Pattern.CASE_INSENSITIVE)),
            new Signal("JNDI_LOOKUP", Pattern.compile("\\$\\{jndi:", Pattern.CASE_INSENSITIVE)),
            new Signal("COMMAND_INJECTION", Pattern.compile("(;|&&|\\|\\|)\\s*(cat|id|whoami|curl|wget|powershell|cmd)\\b", Pattern.CASE_INSENSITIVE))
    );

    private final ClientIpResolver clientIpResolver;

    public SecurityMonitoringFilter(ClientIpResolver clientIpResolver) {
        this.clientIpResolver = clientIpResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long started = System.nanoTime();
        ContentCachingRequestWrapper wrapped = new ContentCachingRequestWrapper(request, MAX_BODY_BYTES_TO_SCAN);

        try {
            filterChain.doFilter(wrapped, response);
        } finally {
            long durationMs = (System.nanoTime() - started) / 1_000_000;
            String method = wrapped.getMethod();
            String path = wrapped.getRequestURI();
            String ip = clientIpResolver.resolve(wrapped);
            int status = response.getStatus();

            log.info("HTTP_REQUEST method={} path={} ip={} status={} durationMs={} userAgent={}",
                    method, path, ip, status, durationMs, safeHeader(wrapped.getHeader("User-Agent")));

            List<String> signals = detectSignals(wrapped);
            if (!signals.isEmpty()) {
                log.warn("INJECTION_ATTEMPT method={} path={} ip={} status={} signals={}",
                        method, path, ip, status, signals);
            }

            if (status == HttpServletResponse.SC_UNAUTHORIZED || status == HttpServletResponse.SC_FORBIDDEN) {
                String event = isSensitivePath(path) ? "CRITICAL_UNAUTHORIZED_ACCESS" : "UNAUTHORIZED_ACCESS";
                log.warn("{} method={} path={} ip={} status={}", event, method, path, ip, status);
            }
        }
    }

    private List<String> detectSignals(ContentCachingRequestWrapper request) {
        String sample = scanSample(request).toLowerCase(Locale.ROOT);
        List<String> detected = new ArrayList<>();
        for (Signal signal : SIGNALS) {
            if (signal.pattern().matcher(sample).find()) {
                detected.add(signal.name());
            }
        }
        return detected;
    }

    private String scanSample(ContentCachingRequestWrapper request) {
        StringBuilder sample = new StringBuilder();
        sample.append(request.getMethod()).append(' ')
                .append(request.getRequestURI()).append(' ');
        if (request.getQueryString() != null) {
            sample.append(request.getQueryString()).append(' ')
                    .append(urlDecode(request.getQueryString())).append(' ');
        }
        byte[] body = request.getContentAsByteArray();
        if (body.length > 0) {
            int length = Math.min(body.length, MAX_BODY_BYTES_TO_SCAN);
            sample.append(new String(body, 0, length, StandardCharsets.UTF_8));
        }
        return sample.toString();
    }

    private String urlDecode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return value;
        }
    }

    private boolean isSensitivePath(String path) {
        return path.startsWith("/api/users")
                || path.startsWith("/api/privacy")
                || path.startsWith("/api/personal-data-access-logs")
                || path.endsWith("/full");
    }

    private String safeHeader(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        String sanitized = value.replaceAll("[\\r\\n\\t]", " ");
        return sanitized.length() <= 512 ? sanitized : sanitized.substring(0, 512);
    }

    private record Signal(String name, Pattern pattern) {}
}
