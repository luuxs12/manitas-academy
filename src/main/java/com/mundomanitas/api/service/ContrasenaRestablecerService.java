package com.mundomanitas.api.service;

import com.mundomanitas.api.config.CorreoProperties;
import com.mundomanitas.api.exception.InvalidoRestablecerTokenException;
import com.mundomanitas.api.model.ContrasenaRestablecerToken;
import com.mundomanitas.api.model.Usuario;
import com.mundomanitas.api.repository.ContrasenaRestablecerTokenRepository;
import com.mundomanitas.api.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Pedido y consumo de los enlaces de restablecimiento.
 *
 * <p>El token viaja en el correo; en la base queda solo su hash SHA-256.
 * Nunca se responde si un correo existe o no: la pantalla muestra el mismo
 * mensaje en los dos casos para no filtrar cuentas.</p>
 */
@Service
public class ContrasenaRestablecerService {

    private final UsuarioRepository users;
    private final ContrasenaRestablecerTokenRepository tokens;
    private final PasswordEncoder contrasenaEncoder;
    private final CorreoService correo;
    private final CorreoProperties properties;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public ContrasenaRestablecerService(
            UsuarioRepository users,
            ContrasenaRestablecerTokenRepository tokens,
            PasswordEncoder contrasenaEncoder,
            CorreoService correo,
            CorreoProperties properties,
            Clock clock
    ) {
        this.users = users;
        this.tokens = tokens;
        this.contrasenaEncoder = contrasenaEncoder;
        this.correo = correo;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Crea un enlace nuevo si la cuenta existe. Borra los anteriores para
     * que solo el último correo sirva.
     */
    @Transactional
    public void requestRestablecer(String email) {
        String normalized = email == null ? "" : email.trim();
        Optional<Usuario> found = users.buscarPorEmailIgnorandoMayusculas(normalized)
                .filter(Usuario::isActivo);

        if (found.isEmpty()) {
            return;
        }

        Usuario usuario = found.get();
        tokens.deleteAllForUsuario(usuario.getId());

        String rawToken = newToken();
        ContrasenaRestablecerToken token = new ContrasenaRestablecerToken(
                usuario,
                hash(rawToken),
                OffsetDateTime.now(clock).plusMinutes(properties.getRestablecerMinutes())
        );
        tokens.save(token);

        correo.sendContrasenaRestablecer(usuario, rawToken);
    }

    /** Para decidir si la pantalla muestra el formulario o el aviso de vencido. */
    @Transactional(readOnly = true)
    public boolean isUsable(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return false;
        }
        return tokens.buscarPorTokenHash(hash(rawToken))
                .filter(token -> token.isUsable(OffsetDateTime.now(clock)))
                .isPresent();
    }

    /** Cambia la contraseña y quema el token en el mismo paso. */
    @Transactional
    public void restablecerContrasena(String rawToken, String newContrasena) {
        ContrasenaRestablecerToken token = tokens.buscarPorTokenHash(hash(rawToken))
                .orElseThrow(InvalidoRestablecerTokenException::new);

        OffsetDateTime now = OffsetDateTime.now(clock);
        if (!token.isUsable(now)) {
            throw new InvalidoRestablecerTokenException();
        }

        Usuario usuario = token.getUsuario();
        if (!usuario.isActivo()) {
            throw new InvalidoRestablecerTokenException();
        }

        usuario.changeContrasenaHash(contrasenaEncoder.encode(newContrasena));
        token.markUsed(now);
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException exception) {
            // SHA-256 es obligatorio en cualquier JVM: si falta, no hay forma
            // segura de continuar.
            throw new IllegalStateException("Falta SHA-256 en la JVM", exception);
        }
    }
}
