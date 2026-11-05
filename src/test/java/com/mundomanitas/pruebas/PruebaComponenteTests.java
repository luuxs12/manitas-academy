package com.mundomanitas.pruebas;

import com.mundomanitas.api.ApiExceptionHandler;
import com.mundomanitas.api.AuthApiController;
import com.mundomanitas.dto.api.LoginResponse;
import com.mundomanitas.service.AuthService;
import com.mundomanitas.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de componente.
 *
 * <p>Aísla el controlador {@link AuthApiController} del resto del sistema: se
 * construye solo con sus colaboradores simulados (Mockito) y un MockMvc
 * independiente. No arranca Spring ni toca la base de datos.</p>
 */
@DisplayName("Prueba de componente")
class PruebaComponenteTests {

    private AuthService authService;
    private UsuarioService usuarioService;
    private MockMvc mockMvc;

    @BeforeEach
    void prepararElComponente() {
        authService = mock(AuthService.class);
        usuarioService = mock(UsuarioService.class);

        LocalValidatorFactoryBean validador = new LocalValidatorFactoryBean();
        validador.afterPropertiesSet();

        mockMvc = MockMvcBuilders
            .standaloneSetup(new AuthApiController(authService, usuarioService))
            .setControllerAdvice(new ApiExceptionHandler())
            .setValidator(validador)
            .build();
    }

    @Test
    @DisplayName("El componente delega el login en el servicio y devuelve el token")
    void elComponenteDelegaElLoginYDevuelveElToken() throws Exception {
        when(authService.login(any()))
            .thenReturn(new LoginResponse("jwt-de-prueba", "Bearer", "Alumno", 7200));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"alumno@mundomanitas.com","password":"123456"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value("jwt-de-prueba"))
            .andExpect(jsonPath("$.tipo").value("Bearer"))
            .andExpect(jsonPath("$.expiraEnSegundos").value(7200));

        verify(authService).login(any());
    }

    @Test
    @DisplayName("Un correo con formato inválido devuelve 400 sin llamar al servicio")
    void unCorreoInvalidoDevuelve400SinLlamarAlServicio() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"esto-no-es-un-correo","password":"123456"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Datos inválidos"))
            .andExpect(jsonPath("$.detalles.email").exists());

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("Una contraseña en blanco devuelve 400 sin llamar al servicio")
    void unaContrasenaEnBlancoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"alumno@mundomanitas.com","password":" "}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detalles.password").exists());

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("La recuperación responde igual exista o no el correo")
    void laRecuperacionNoRevelaSiElCorreoExiste() throws Exception {
        mockMvc.perform(post("/api/auth/recuperar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"desconocido@mundomanitas.com"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mensaje").value(
                "Si el correo está registrado, enviamos un enlace de recuperación."
            ));

        verify(usuarioService).solicitarRecuperacion("desconocido@mundomanitas.com");
    }

    @Test
    @DisplayName("El restablecimiento exige token y contraseña")
    void elRestablecimientoExigeTokenYContrasena() throws Exception {
        mockMvc.perform(post("/api/auth/restablecer")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"token":"","contrasena":""}
                    """))
            .andExpect(status().isBadRequest());

        verify(usuarioService, never()).restablecer(any(), any());
    }
}
