package com.mundomanitas.api.service.admin;

import com.mundomanitas.api.model.Usuario;
import com.mundomanitas.api.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

/** Crea el admin una sola vez; nunca pisa la clave de una cuenta ya existente. */
@Service
public class AdminBootstrapServiceImpl implements AdminBootstrapService {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapServiceImpl.class);

    private final UsuarioRepository users;
    private final PasswordEncoder encoder;

    public AdminBootstrapServiceImpl(UsuarioRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void ensureAdmin(String email, String contrasena) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (normalizedEmail.isEmpty()) {
            throw new IllegalArgumentException("El correo del administrador no puede estar vacío.");
        }
        if (contrasena == null || contrasena.isBlank()) {
            throw new IllegalArgumentException("La contraseña del administrador no puede estar vacía.");
        }

        Optional<Usuario> existing = users.buscarPorEmailIgnorandoMayusculas(normalizedEmail);
        if (existing.isPresent()) {
            Usuario usuario = existing.get();
            if (!"ADMIN".equals(usuario.getRol())) {
                usuario.changeRol("ADMIN");
                users.save(usuario);
                log.info("Cuenta administradora promovida email={}", normalizedEmail);
            } else {
                log.info("Cuenta administradora ya disponible email={}", normalizedEmail);
            }
            return;
        }

        Usuario admin = new Usuario(normalizedEmail, encoder.encode(contrasena), "Administrador");
        admin.changeRol("ADMIN");
        users.save(admin);
        log.info("Cuenta administradora creada email={}", normalizedEmail);
    }
}
