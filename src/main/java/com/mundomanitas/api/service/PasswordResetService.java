package com.mundomanitas.api.service;

import com.mundomanitas.api.config.MailProperties;
import com.mundomanitas.api.exception.InvalidResetTokenException;
import com.mundomanitas.api.model.PasswordResetToken;
import com.mundomanitas.api.model.User;
import com.mundomanitas.api.repository.PasswordResetTokenRepository;
import com.mundomanitas.api.repository.UserRepository;
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
public class PasswordResetService {

    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final MailService mail;
    private final MailProperties properties;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetService(
            UserRepository users,
            PasswordResetTokenRepository tokens,
            PasswordEncoder passwordEncoder,
            MailService mail,
            MailProperties properties,
            Clock clock
    ) {
        this.users = users;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
        this.mail = mail;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Crea un enlace nuevo si la cuenta existe. Borra los anteriores para
     * que solo el último correo sirva.
     */
    @Transactional
    public void requestReset(String email) {
        String normalized = email == null ? "" : email.trim();
        Optional<User> found = users.findByEmailIgnoreCase(normalized)
                .filter(User::isActive);

        if (found.isEmpty()) {
            return;
        }

        User user = found.get();
        tokens.deleteAllForUser(user.getId());

        String rawToken = newToken();
        PasswordResetToken token = new PasswordResetToken(
                user,
                hash(rawToken),
                OffsetDateTime.now(clock).plusMinutes(properties.getResetMinutes())
        );
        tokens.save(token);

        mail.sendPasswordReset(user, rawToken);
    }

    /** Para decidir si la pantalla muestra el formulario o el aviso de vencido. */
    @Transactional(readOnly = true)
    public boolean isUsable(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return false;
        }
        return tokens.findByTokenHash(hash(rawToken))
                .filter(token -> token.isUsable(OffsetDateTime.now(clock)))
                .isPresent();
    }

    /** Cambia la contraseña y quema el token en el mismo paso. */
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        PasswordResetToken token = tokens.findByTokenHash(hash(rawToken))
                .orElseThrow(InvalidResetTokenException::new);

        OffsetDateTime now = OffsetDateTime.now(clock);
        if (!token.isUsable(now)) {
            throw new InvalidResetTokenException();
        }

        User user = token.getUser();
        if (!user.isActive()) {
            throw new InvalidResetTokenException();
        }

        user.changePasswordHash(passwordEncoder.encode(newPassword));
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
