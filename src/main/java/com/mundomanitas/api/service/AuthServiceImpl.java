package com.mundomanitas.api.service;

import com.mundomanitas.api.dto.RegisterForm;
import com.mundomanitas.api.exception.DuplicateEmailException;
import com.mundomanitas.api.model.User;
import com.mundomanitas.api.repository.UserRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/** Implementación del registro de cuentas. */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public User register(RegisterForm form) {
        String email = StringUtils.trimToEmpty(form.getEmail()).toLowerCase(Locale.ROOT);

        if (users.existsByEmailIgnoreCase(email)) {
            log.info("Registro rechazado: el correo ya existe");
            throw new DuplicateEmailException(email);
        }

        User user = new User(
                email,
                passwordEncoder.encode(form.getPassword()),
                StringUtils.trimToEmpty(form.getFullName())
        );

        User saved = users.save(user);
        log.info("Cuenta registrada id={}", saved.getId());
        return saved;
    }
}
