package com.mundomanitas.api.service;

import com.mundomanitas.api.model.Usuario;
import com.mundomanitas.api.repository.UsuarioRepository;
import com.mundomanitas.api.service.admin.AdminBootstrapService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** La cuenta debe quedar realmente en PostgreSQL con una clave verificable. */
@SpringBootTest
@Transactional
@DisplayName("Cuenta administradora permanente")
class AdminBootstrapBaseTest {

    @Autowired private AdminBootstrapService bootstrap;
    @Autowired private UsuarioRepository users;
    @Autowired private PasswordEncoder encoder;

    @Test
    @DisplayName("guarda el admin con rol ADMIN y clave BCrypt en la base real")
    void createsAdminInBase() {
        String email = "admin-bootstrap-" + UUID.randomUUID() + "@ejemplo.test";

        bootstrap.ensureAdmin(email, "Clave12345!");

        Usuario saved = users.buscarPorEmailIgnorandoMayusculas(email).orElseThrow();
        assertThat(saved.getRol()).isEqualTo("ADMIN");
        assertThat(saved.getContrasenaHash()).isNotEqualTo("Clave12345!");
        assertThat(encoder.matches("Clave12345!", saved.getContrasenaHash())).isTrue();
    }
}
