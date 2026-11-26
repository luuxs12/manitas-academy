package com.mundomanitas.api.config;

import com.mundomanitas.api.security.InMemoryRateLimiter;
import com.mundomanitas.api.security.RateLimiter;
import com.mundomanitas.api.security.RateLimiter.Decision;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.Cookie;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Exposición pública y límite de intentos sobre rutas sensibles. */
@SpringBootTest(properties = {
        "app.monitoring.username=monitor",
        "app.monitoring.password=monitor-secret",
        "app.stripe.webhook-secret=secreto-de-prueba"
})
@AutoConfigureMockMvc
@DisplayName("Exposición de datos y límite de intentos")
class DatosExposureAndRateLimitTest {

    @Autowired private MockMvc mvc;
    @Autowired private ServerProperties serverProperties;

    @Nested
    @DisplayName("prueba de componentes")
    class Componentes {

        @Test
        @DisplayName("el limitador cuenta por clave, informa el reintento y se reinicia por ventana")
        void limiterCountsPerClaveAndVentana() {
            RateLimiter limiter = new InMemoryRateLimiter(3, Duration.ofSeconds(60));
            Instant start = Instant.parse("2026-11-25T00:00:00Z");

            assertThat(limiter.verificacion("ip-a", start).allowed()).isTrue();
            assertThat(limiter.verificacion("ip-a", start.plusSeconds(1)).allowed()).isTrue();
            assertThat(limiter.verificacion("ip-a", start.plusSeconds(2)).allowed()).isTrue();

            Decision blocked = limiter.verificacion("ip-a", start.plusSeconds(5));
            assertThat(blocked.allowed()).isFalse();
            assertThat(blocked.retryDespuesSegundos()).isBetween(1L, 60L);
            assertThat(limiter.verificacion("ip-b", start).allowed()).isTrue();
            assertThat(limiter.verificacion("ip-a", start.plusSeconds(61)).allowed()).isTrue();
        }
    }

    @Nested
    @DisplayName("prueba de integración")
    class Integracion {

        @Test
        @DisplayName("lo público se lee sin sesión y lo privado pide acceso")
        void publicPagesStayOpenAndPrivateAreasAskForLogin() throws Exception {
            for (String path : List.of("/", "/nosotros", "/cursos", "/buscar",
                    "/registro", "/recuperar", "/terminos", "/privacidad")) {
                mvc.perform(get(path)).andExpect(status().isOk());
            }

            assertThat(redirectOf(get("/panel"))).contains("login");
            assertThat(redirectOf(get("/panel/perfil"))).contains("login");
            assertThat(redirectOf(get("/admin"))).contains("login");
            assertThat(redirectOf(get("/admin/usuarios"))).contains("login");
        }

        @Test
        @DisplayName("las respuestas públicas llevan cabeceras que reducen filtraciones")
        void securityHeadersArePresent() throws Exception {
            var response = mvc.perform(get("/")).andReturn().getResponse();

            assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
            assertThat(response.getHeader("X-Frame-Options")).isEqualTo("DENY");
            assertThat(response.getHeader("Referrer-Policy"))
                    .isEqualTo("strict-origin-when-cross-origin");
            assertThat(response.getHeader("Permissions-Policy"))
                    .contains("camera=()", "microphone=()", "geolocation=()", "payment=()");
        }

        @Test
        @DisplayName("la cookie de sesión pide SameSite=Lax y sigue siendo HttpOnly")
        void sesionCookieIsHardened() {
            var cookie = serverProperties.getServlet().getSession().getCookie();

            assertThat(cookie.getSameSite()).isEqualTo(Cookie.SameSite.LAX);
        }

        @Test
        @DisplayName("el catálogo público no filtra correos ni campos internos")
        void publicCatalogDoesNotLeakAccounts() throws Exception {
            String html = mvc.perform(get("/cursos")).andReturn()
                    .getResponse().getContentAsString();

            assertThat(html).doesNotContain(
                    "password_hash", "stripe_customer_id", "admin@damianprint.com");
        }

        @Test
        @DisplayName("Actuator solo responde al usuario de monitoreo")
        void actuatorStaysPrivate() throws Exception {
            mvc.perform(get("/actuator/health")).andExpect(status().isUnauthorized());
            mvc.perform(get("/actuator/prometheus")).andExpect(status().isUnauthorized());
            mvc.perform(get("/actuator/health")
                            .header("Authorization", basic("monitor", "monitor-secret")))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("el webhook público exige firma Stripe válida")
        void webhookRequiresSignature() throws Exception {
            String body = "{\"id\":\"evt_prueba\",\"type\":\"prueba.sin_efecto\"}";

            mvc.perform(post("/pagos/stripe/webhook")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());

            mvc.perform(post("/pagos/stripe/webhook")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Stripe-Signature", signature(body))
                            .content(body))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("diez intentos fallidos de login se cortan con 429 y otra IP sigue libre")
        void repeatedLoginAttemptsAreLimited() throws Exception {
            String ip = "203.0.113.77";
            MvcResult page = mvc.perform(get("/?login").with(ip(ip))).andReturn();
            MockHttpSession session = (MockHttpSession) page.getRequest().getSession(false);
            String token = csrfToken(page.getResponse().getContentAsString());

            for (int intento = 0; intento < 10; intento++) {
                mvc.perform(login(session, token, ip))
                        .andExpect(status().is3xxRedirection());
            }

            MvcResult blocked = mvc.perform(login(session, token, ip))
                    .andExpect(status().is(429))
                    .andReturn();
            assertThat(blocked.getResponse().getHeader("Retry-After")).isNotBlank();

            String otherIp = "203.0.113.78";
            MvcResult otherPage = mvc.perform(get("/?login").with(ip(otherIp))).andReturn();
            MockHttpSession otherSesion = (MockHttpSession) otherPage.getRequest().getSession(false);
            String otherToken = csrfToken(otherPage.getResponse().getContentAsString());
            mvc.perform(login(otherSesion, otherToken, otherIp))
                    .andExpect(status().is3xxRedirection());
        }
    }

    private MockHttpServletRequestBuilder login(MockHttpSession session, String token, String ip) {
        return post("/login").session(session)
                .param("_csrf", token)
                .param("username", "nadie@ejemplo.test")
                .param("contrasena", "Incorrecta123")
                .with(ip(ip));
    }

    private String redirectOf(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
            throws Exception {
        return mvc.perform(request).andReturn().getResponse().getRedirectedUrl();
    }

    private String csrfToken(String html) {
        var matcher = java.util.regex.Pattern
                .compile("name=\"_csrf\"\\s+value=\"([^\"]+)\"")
                .matcher(html);
        assertThat(matcher.find()).as("la página incluye el token CSRF").isTrue();
        return matcher.group(1);
    }

    private String signature(String body) throws Exception {
        String timestamp = "1700000000";
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("secreto-de-prueba".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signed = (timestamp + "." + body).getBytes(StandardCharsets.UTF_8);
        return "t=" + timestamp + ",v1=" + HexFormat.of().formatHex(mac.doFinal(signed));
    }

    private String basic(String usuario, String contrasena) {
        return "Basic " + java.util.Base64.getEncoder().encodeToString(
                (usuario + ":" + contrasena).getBytes(StandardCharsets.UTF_8));
    }

    private RequestPostProcessor ip(String address) {
        return request -> {
            request.setRemoteAddr(address);
            return request;
        };
    }
}
