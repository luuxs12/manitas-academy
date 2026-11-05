package com.mundomanitas.pruebas;

import com.jayway.jsonpath.JsonPath;
import com.mundomanitas.repository.ModuloRepository;
import com.mundomanitas.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba de integración.
 *
 * <p>Levanta la aplicación completa en un puerto aleatorio y ejercita el flujo
 * real por HTTP: controladores, filtros de seguridad, servicios, repositorios y
 * base de datos H2. No usa mocks ni MockMvc.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("Prueba de integración")
class PruebaIntegracionTests {

    private static final String LOGIN = "/api/auth/login";
    private static final String CATALOGO = "/api/cursos?page=0&size=9";

    @LocalServerPort
    private int puerto;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ModuloRepository moduloRepository;

    private final HttpClient cliente = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    private String url(String ruta) {
        return "http://localhost:" + puerto + ruta;
    }

    private HttpResponse<String> login(String email, String password) throws Exception {
        String cuerpo = """
            {"email":"%s","password":"%s"}
            """.formatted(email, password);

        HttpRequest peticion = HttpRequest.newBuilder(URI.create(url(LOGIN)))
            .timeout(Duration.ofSeconds(20))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(cuerpo))
            .build();

        return cliente.send(peticion, HttpResponse.BodyHandlers.ofString());
    }

    private String tokenValido() throws Exception {
        HttpResponse<String> respuesta = login("alumno@mundomanitas.com", "123456");
        assertEquals(200, respuesta.statusCode(), "el login debe responder 200");

        String token = JsonPath.read(respuesta.body(), "$.token");
        assertFalse(token.isBlank(), "el token no debe estar vacío");
        return token;
    }

    private HttpResponse<String> catalogoCon(String token) throws Exception {
        HttpRequest peticion = HttpRequest.newBuilder(URI.create(url(CATALOGO)))
            .timeout(Duration.ofSeconds(20))
            .header("Accept", "application/json")
            .header("Authorization", "Bearer " + token)
            .GET()
            .build();

        return cliente.send(peticion, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    @DisplayName("El login real emite un JWT y con él se consulta el catálogo")
    void elLoginRealEmiteUnJwtYConElSeConsultaElCatalogo() throws Exception {
        HttpResponse<String> catalogo = catalogoCon(tokenValido());

        assertEquals(200, catalogo.statusCode());
        assertTrue(catalogo.body().contains("\"content\""),
            "la respuesta debe ser una página de cursos");
    }

    @Test
    @DisplayName("Sin token el catálogo responde 401 en JSON")
    void sinTokenElCatalogoResponde401() throws Exception {
        HttpRequest peticion = HttpRequest.newBuilder(URI.create(url(CATALOGO)))
            .timeout(Duration.ofSeconds(20))
            .header("Accept", "application/json")
            .GET()
            .build();

        HttpResponse<String> respuesta = cliente.send(peticion, HttpResponse.BodyHandlers.ofString());

        assertEquals(401, respuesta.statusCode());
        assertTrue(respuesta.body().contains("No autenticado"));
    }

    @Test
    @DisplayName("Con un token inválido el catálogo responde 401")
    void conTokenInvalidoElCatalogoResponde401() throws Exception {
        HttpResponse<String> respuesta = catalogoCon("token.invalido.firmado");

        assertEquals(401, respuesta.statusCode());
    }

    @Test
    @DisplayName("Con credenciales incorrectas no se emite token")
    void conCredencialesIncorrectasNoSeEmiteToken() throws Exception {
        HttpResponse<String> respuesta = login("alumno@mundomanitas.com", "clave-incorrecta");

        assertEquals(401, respuesta.statusCode());
        assertFalse(respuesta.body().contains("\"token\""), "no debe exponerse un token");
    }

    @Test
    @DisplayName("Los usuarios y módulos demo están persistidos en la base de datos")
    void losDatosDemoEstanPersistidos() {
        assertTrue(usuarioRepository.findByEmailIgnoreCase("alumno@mundomanitas.com").isPresent(),
            "el usuario demo debe existir en la base");
        assertTrue(usuarioRepository.findByEmailIgnoreCase("docente@mundomanitas.com").isPresent());
        assertTrue(moduloRepository.count() > 0, "debe haber módulos sembrados");

        assertTrue(usuarioRepository.findByEmailIgnoreCase("noexiste@mundomanitas.com").isEmpty(),
            "un correo inexistente no debe aparecer");
    }
}
