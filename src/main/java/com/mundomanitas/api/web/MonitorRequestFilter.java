package com.mundomanitas.api.web;

import com.mundomanitas.api.dto.admin.MonitorMuestra;
import com.mundomanitas.api.service.admin.MonitorService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Mide las peticiones que realmente sirve la aplicación. No toca archivos
 * estáticos ni la propia API del monitor, para que el panel no se mida a sí
 * mismo. El requestId es el mismo que ya usan los logs.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class MonitorRequestFilter extends OncePerRequestFilter {

    private static final List<String> OMITIR = List.of(
            "/js/", "/css/", "/img/", "/uploads/", "/webjars/", "/actuator/",
            "/admin/api/monitoreo", "/favicon.ico", "/error");
    private static final Pattern ID = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}|[0-9]+");

    private final MonitorService monitor;

    /**
     * El contenedor crea los filtros antes de que exista el EntityManagerFactory;
     * diferir el servicio evita forzar las consultas JPA en ese momento.
     */
    public MonitorRequestFilter(@Lazy MonitorService monitor) {
        this.monitor = monitor;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {
        long inicio = System.nanoTime();
        int estado = 200;
        boolean medir = debeMedir(request.getRequestURI());
        String ruta = medir ? normalizar(request.getRequestURI()) : "";
        String metodo = request.getMethod();
        String requestId = MDC.get(RequestIdFilter.MDC_KEY);
        try {
            chain.doFilter(request, response);
            estado = response.getStatus();
        } catch (ServletException | IOException exception) {
            estado = 500;
            throw exception;
        } finally {
            if (medir) {
                long duracion = Math.max(1, (System.nanoTime() - inicio) / 1_000_000L);
                monitor.registrar(new MonitorMuestra(
                        Instant.now(), metodo, ruta, estado, duracion, requestId));
            }
        }
    }

    private boolean debeMedir(String uri) {
        if (uri == null) return false;
        return OMITIR.stream().noneMatch(uri::startsWith);
    }

    /** Agrupa identificadores concretos para que las rutas del panel sean legibles. */
    private String normalizar(String uri) {
        return ID.matcher(uri).replaceAll("{}");
    }
}
