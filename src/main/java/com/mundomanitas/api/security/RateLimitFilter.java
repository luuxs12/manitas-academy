package com.mundomanitas.api.security;

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

import java.io.IOException;
import java.time.Instant;
import java.util.Set;

/**
 * Frena el abuso de formularios de acceso y recuperación antes de que lleguen
 * a Spring Security. La clave es la IP real del request, nunca un encabezado
 * que el cliente pueda inventar.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> LIMITED_PATHS = Set.of(
            "/login", "/registro", "/recuperar", "/restablecer");
    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final RateLimiter limiter;

    public RateLimitFilter(RateLimiter limiter) {
        this.limiter = limiter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod())
                || !LIMITED_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String ip = request.getRemoteAddr();
        String path = request.getRequestURI();
        RateLimiter.Decision decision = limiter.check(ip + "|" + path, Instant.now());

        if (!decision.allowed()) {
            log.warn("Límite de intentos alcanzado ip={} ruta={} reintento={}s",
                    ip, path, decision.retryAfterSeconds());
            response.setStatus(429);
            response.setHeader("Retry-After", Long.toString(decision.retryAfterSeconds()));
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("Demasiados intentos. Probá de nuevo en unos segundos.");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
