package com.mundomanitas.api.web;

import com.mundomanitas.api.dto.admin.MonitorMuestra;
import com.mundomanitas.api.service.admin.MonitorService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import org.springframework.context.annotation.Lazy;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.lang.reflect.Constructor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@DisplayName("Medición real de peticiones")
class MonitorRequestFilterTest {

    private MonitorService monitor;
    private MonitorRequestFilter filter;

    @BeforeEach
    void setUp() {
        monitor = mock(MonitorService.class);
        filter = new MonitorRequestFilter(monitor);
        MDC.put(RequestIdFilter.MDC_KEY, "req-123");
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("difiere el servicio para no crear el repositorio JPA al arrancar")
    void servicioDiferido() throws NoSuchMethodException {
        Constructor<MonitorRequestFilter> constructor =
                MonitorRequestFilter.class.getConstructor(MonitorService.class);

        assertThat(constructor.getParameters()[0].isAnnotationPresent(Lazy.class)).isTrue();
    }

    @Test
    @DisplayName("registra estado, tiempo, ruta agrupada y requestId")
    void registraPeticion() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/admin/usuarios/123e4567-e89b-12d3-a456-426614174000");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(503);

        filter.doFilter(request, response, (req, res) -> { });

        ArgumentCaptor<MonitorMuestra> captor = ArgumentCaptor.forClass(MonitorMuestra.class);
        verify(monitor).registrar(captor.capture());
        MonitorMuestra muestra = captor.getValue();
        assertThat(muestra.estado()).isEqualTo(503);
        assertThat(muestra.metodo()).isEqualTo("GET");
        assertThat(muestra.ruta()).isEqualTo("/admin/usuarios/{}");
        assertThat(muestra.requestId()).isEqualTo("req-123");
        assertThat(muestra.duracionMs()).isPositive();
    }

    @Test
    @DisplayName("no se mide a sí mismo ni a los archivos estáticos")
    void omiteRutasInternas() throws Exception {
        for (String ruta : Lista.rutas()) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", ruta);
            filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> { });
        }
        verifyNoInteractions(monitor);
    }

    @Test
    @DisplayName("una excepción se cuenta como error 500 y se propaga")
    void registraErrorAntesDePropagar() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/panel/cursos/python");
        FilterChain falla = (req, res) -> {
            throw new IOException("falló");

        };

        assertThatThrownBy(() -> filter.doFilter(request, new MockHttpServletResponse(), falla))
                .isInstanceOf(IOException.class);

        ArgumentCaptor<MonitorMuestra> captor = ArgumentCaptor.forClass(MonitorMuestra.class);
        verify(monitor).registrar(captor.capture());
        assertThat(captor.getValue().estado()).isEqualTo(500);
        assertThat(captor.getValue().ruta()).isEqualTo("/panel/cursos/python");
    }

    private static final class Lista {
        static java.util.List<String> rutas() {
            return java.util.List.of(
                    "/admin/api/monitoreo", "/js/monitoreo.js", "/css/styles.css",
                    "/img/brand/favicon.png", "/actuator/health", "/favicon.ico");
        }
    }
}
