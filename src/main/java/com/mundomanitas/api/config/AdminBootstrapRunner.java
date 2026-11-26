package com.mundomanitas.api.config;

import com.mundomanitas.api.service.admin.AdminBootstrapService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Crea la cuenta ADMIN al arrancar cuando se configura ADMIN_PASSWORD. */
@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final AdminBootstrapService bootstrap;
    private final String email;
    private final String contrasena;

    public AdminBootstrapRunner(AdminBootstrapService bootstrap,
                                @Value("${app.admin.email:admin@damianprint.com}") String email,
                                @Value("${app.admin.password:}") String contrasena) {
        this.bootstrap = bootstrap;
        this.email = email;
        this.contrasena = contrasena;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (contrasena == null || contrasena.isBlank()) {
            log.info("Sin ADMIN_PASSWORD: se conserva la cuenta administradora existente.");
            return;
        }
        bootstrap.ensureAdmin(email, contrasena);
    }
}
