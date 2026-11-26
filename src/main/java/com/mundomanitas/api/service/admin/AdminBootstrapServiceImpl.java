package com.mundomanitas.api.service.admin;

import com.mundomanitas.api.model.User;
import com.mundomanitas.api.repository.UserRepository;
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

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public AdminBootstrapServiceImpl(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void ensureAdmin(String email, String password) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (normalizedEmail.isEmpty()) {
            throw new IllegalArgumentException("El correo del administrador no puede estar vacío.");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("La contraseña del administrador no puede estar vacía.");
        }

        Optional<User> existing = users.findByEmailIgnoreCase(normalizedEmail);
        if (existing.isPresent()) {
            User user = existing.get();
            if (!"ADMIN".equals(user.getRole())) {
                user.changeRole("ADMIN");
                users.save(user);
                log.info("Cuenta administradora promovida email={}", normalizedEmail);
            } else {
                log.info("Cuenta administradora ya disponible email={}", normalizedEmail);
            }
            return;
        }

        User admin = new User(normalizedEmail, encoder.encode(password), "Administrador");
        admin.changeRole("ADMIN");
        users.save(admin);
        log.info("Cuenta administradora creada email={}", normalizedEmail);
    }
}
