package com.mundomanitas.api.service;

import com.mundomanitas.api.dto.CuentaDeleteFormulario;
import com.mundomanitas.api.dto.ContrasenaChangeFormulario;
import com.mundomanitas.api.dto.PreferenciasFormulario;
import com.mundomanitas.api.dto.PerfilFormulario;
import com.mundomanitas.api.exception.DuplicadoEmailException;
import com.mundomanitas.api.exception.ContrasenaNoCoincideException;
import com.mundomanitas.api.exception.IncorrectaContrasenaException;
import com.mundomanitas.api.model.Usuario;
import com.mundomanitas.api.repository.ContrasenaRestablecerTokenRepository;
import com.mundomanitas.api.repository.UsuarioRepository;
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
public class CuentaService {

    private final UsuarioRepository users;
    private final ContrasenaRestablecerTokenRepository restablecerTokens;
    private final PasswordEncoder contrasenaEncoder;
    private final AvatarStorage avatars;

    public CuentaService(
            UsuarioRepository users,
            ContrasenaRestablecerTokenRepository restablecerTokens,
            PasswordEncoder contrasenaEncoder,
            AvatarStorage avatars
    ) {
        this.users = users;
        this.restablecerTokens = restablecerTokens;
        this.contrasenaEncoder = contrasenaEncoder;
        this.avatars = avatars;
    }

    @Transactional(readOnly = true)
    public Usuario require(UUID id) {
        return users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "La cuenta ya no existe"));
    }

    @Transactional
    public Usuario updatePerfil(Usuario usuario, PerfilFormulario formulario) {
        String nombreCompleto = formulario.getNombreCompleto().trim();
        String email = formulario.getEmail().trim().toLowerCase(Locale.ROOT);

        users.buscarPorEmailIgnorandoMayusculas(email)
                .filter(other -> !other.getId().equals(usuario.getId()))
                .ifPresent(other -> {
                    throw new DuplicadoEmailException(email);
                });

        usuario.changeCompletoNombre(nombreCompleto);
        usuario.changeEmail(email);
        return users.save(usuario);
    }

    @Transactional
    public void changeContrasena(Usuario usuario, ContrasenaChangeFormulario formulario) {
        if (!contrasenaEncoder.matches(formulario.getActualContrasena(), usuario.getContrasenaHash())) {
            throw new IncorrectaContrasenaException();
        }
        if (!formulario.getNewContrasena().equals(formulario.getConfirmContrasena())) {
            throw new ContrasenaNoCoincideException();
        }

        usuario.changeContrasenaHash(contrasenaEncoder.encode(formulario.getNewContrasena()));
        users.save(usuario);
        // Una contraseña nueva invalida los enlaces que pudieran quedar abiertos.
        restablecerTokens.deleteAllForUsuario(usuario.getId());
    }

    @Transactional
    public void updateAvatar(Usuario usuario, MultipartFile file) {
        String previous = usuario.getAvatarPath();
        String stored = avatars.store(file);
        usuario.changeAvatarPath(stored);
        users.save(usuario);
        if (previous != null && !previous.equals(stored)) {
            avatars.delete(previous);
        }
    }

    @Transactional
    public void removeAvatar(Usuario usuario) {
        String previous = usuario.getAvatarPath();
        if (previous == null) return;
        usuario.changeAvatarPath(null);
        users.save(usuario);
        avatars.delete(previous);
    }

    @Transactional
    public void savePreferencias(Usuario usuario, PreferenciasFormulario formulario) {
        usuario.changePreferencias(formulario.getTheme(), formulario.isNotifyEmail());
        users.save(usuario);
    }

    @Transactional
    public boolean deleteCuenta(Usuario usuario, CuentaDeleteFormulario formulario) {
        if (!formulario.isConfirmed()) {
            return false;
        }
        restablecerTokens.deleteAllForUsuario(usuario.getId());
        String avatar = usuario.getAvatarPath();
        users.delete(usuario);
        if (avatar != null) {
            avatars.delete(avatar);
        }
        return true;
    }
}
