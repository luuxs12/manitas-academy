package com.mundomanitas.api.service;

import com.mundomanitas.api.dto.RegistroFormulario;
import com.mundomanitas.api.exception.DuplicadoEmailException;
import com.mundomanitas.api.model.Usuario;
import com.mundomanitas.api.repository.UsuarioRepository;
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

    private final UsuarioRepository users;
    private final PasswordEncoder contrasenaEncoder;

    public AuthServiceImpl(UsuarioRepository users, PasswordEncoder contrasenaEncoder) {
        this.users = users;
        this.contrasenaEncoder = contrasenaEncoder;
    }

    @Override
    @Transactional
    public Usuario registro(RegistroFormulario formulario) {
        String email = StringUtils.trimToEmpty(formulario.getEmail()).toLowerCase(Locale.ROOT);

        if (users.existePorEmailIgnorandoMayusculas(email)) {
            log.info("Registro rechazado: el correo ya existe");
            throw new DuplicadoEmailException(email);
        }

        Usuario usuario = new Usuario(
                email,
                contrasenaEncoder.encode(formulario.getContrasena()),
                StringUtils.trimToEmpty(formulario.getNombreCompleto())
        );

        Usuario saved = users.save(usuario);
        log.info("Cuenta registrada id={}", saved.getId());
        return saved;
    }
}
