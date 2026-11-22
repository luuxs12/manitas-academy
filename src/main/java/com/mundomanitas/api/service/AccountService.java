package com.mundomanitas.api.service;

import com.mundomanitas.api.dto.AccountDeleteForm;
import com.mundomanitas.api.dto.PasswordChangeForm;
import com.mundomanitas.api.dto.PreferencesForm;
import com.mundomanitas.api.dto.ProfileForm;
import com.mundomanitas.api.exception.DuplicateEmailException;
import com.mundomanitas.api.exception.PasswordMismatchException;
import com.mundomanitas.api.exception.WrongPasswordException;
import com.mundomanitas.api.model.User;
import com.mundomanitas.api.repository.PasswordResetTokenRepository;
import com.mundomanitas.api.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.UUID;

/**
 * Operaciones sobre la cuenta de la persona autenticada.
 *
 * <p>Todo lo que cambia acá se guarda en PostgreSQL: no hay datos de
 * perfil, plan ni contraseña que vivan solo en la sesión.</p>
 */
@Service
public class AccountService {

    private final UserRepository users;
    private final PasswordResetTokenRepository resetTokens;
    private final PasswordEncoder passwordEncoder;
    private final AvatarStorage avatars;

    public AccountService(
            UserRepository users,
            PasswordResetTokenRepository resetTokens,
            PasswordEncoder passwordEncoder,
            AvatarStorage avatars
    ) {
        this.users = users;
        this.resetTokens = resetTokens;
        this.passwordEncoder = passwordEncoder;
        this.avatars = avatars;
    }

    @Transactional(readOnly = true)
    public User require(UUID id) {
        return users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "La cuenta ya no existe"));
    }

    @Transactional
    public User updateProfile(User user, ProfileForm form) {
        String fullName = form.getFullName().trim();
        String email = form.getEmail().trim().toLowerCase(Locale.ROOT);

        users.findByEmailIgnoreCase(email)
                .filter(other -> !other.getId().equals(user.getId()))
                .ifPresent(other -> {
                    throw new DuplicateEmailException(email);
                });

        user.changeFullName(fullName);
        user.changeEmail(email);
        return users.save(user);
    }

    @Transactional
    public void changePassword(User user, PasswordChangeForm form) {
        if (!passwordEncoder.matches(form.getCurrentPassword(), user.getPasswordHash())) {
            throw new WrongPasswordException();
        }
        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            throw new PasswordMismatchException();
        }

        user.changePasswordHash(passwordEncoder.encode(form.getNewPassword()));
        users.save(user);
        // Una contraseña nueva invalida los enlaces que pudieran quedar abiertos.
        resetTokens.deleteAllForUser(user.getId());
    }

    @Transactional
    public void updateAvatar(User user, MultipartFile file) {
        String previous = user.getAvatarPath();
        String stored = avatars.store(file);
        user.changeAvatarPath(stored);
        users.save(user);
        if (previous != null && !previous.equals(stored)) {
            avatars.delete(previous);
        }
    }

    @Transactional
    public void removeAvatar(User user) {
        String previous = user.getAvatarPath();
        if (previous == null) return;
        user.changeAvatarPath(null);
        users.save(user);
        avatars.delete(previous);
    }

    @Transactional
    public void savePreferences(User user, PreferencesForm form) {
        user.changePreferences(form.getTheme(), form.isNotifyEmail());
        users.save(user);
    }

    @Transactional
    public boolean deleteAccount(User user, AccountDeleteForm form) {
        if (!form.isConfirmed()) {
            return false;
        }
        resetTokens.deleteAllForUser(user.getId());
        String avatar = user.getAvatarPath();
        users.delete(user);
        if (avatar != null) {
            avatars.delete(avatar);
        }
        return true;
    }
}
