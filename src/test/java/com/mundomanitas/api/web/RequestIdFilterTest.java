package com.mundomanitas.api.web;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Identificador de request en logs")
class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("reutiliza un X-Request-Id válido y lo devuelve en la respuesta")
    void reusesValidRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "req-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        String[] visto = new String[1];
        FilterChain chain = (req, res) -> visto[0] = MDC.get("requestId");

        filter.doFilter(request, response, chain);

        assertThat(visto[0]).isEqualTo("req-123");
        assertThat(response.getHeader("X-Request-Id")).isEqualTo("req-123");
        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    @DisplayName("genera un id cuando no viene uno en la petición")
    void generatesRequestIdWhenMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        String[] visto = new String[1];

        filter.doFilter(request, response, (req, res) -> visto[0] = MDC.get("requestId"));

        assertThat(visto[0]).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        assertThat(response.getHeader("X-Request-Id")).isEqualTo(visto[0]);
    }

    @Test
    @DisplayName("descarta un id malformado en lugar de copiarlo a los logs")
    void rejectsMalformedRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "malformed id with spaces\nand newline");
        MockHttpServletResponse response = new MockHttpServletResponse();
        String[] visto = new String[1];

        filter.doFilter(request, response, (req, res) -> visto[0] = MDC.get("requestId"));

        assertThat(visto[0]).isNotEqualTo(request.getHeader("X-Request-Id"));
        assertThat(visto[0]).matches("[0-9a-f-]{36}");
        assertThat(response.getHeader("X-Request-Id")).isEqualTo(visto[0]);
    }

    @Test
    @DisplayName("limpia el MDC aunque la cadena lance una excepción")
    void clearsMdcOnFailure() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                filter.doFilter(request, response, (req, res) -> {
                    throw new IllegalStateException("boom");
                }))
                .isInstanceOf(IllegalStateException.class);

        assertThat(MDC.get("requestId")).isNull();
    }
}
