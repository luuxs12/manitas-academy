package com.mundomanitas.api.service;

import com.mundomanitas.api.dto.UsuarioActual;
import com.mundomanitas.api.model.Usuario;
import com.mundomanitas.api.repository.UsuarioRepository;
import com.mundomanitas.api.security.AutenticadoUsuario;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementación de la carga del usuario actual para las vistas. */
@Service
public class ActualUsuarioServiceImpl implements ActualUsuarioService {

    private final UsuarioRepository users;
    private final NotificacionService notifications;

    public ActualUsuarioServiceImpl(UsuarioRepository users, NotificacionService notifications) {
        this.users = users;
        this.notifications = notifications;
    }

    @Override
    @Transactional(readOnly = true)
    public SesionVista load(Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof AutenticadoUsuario principal)) {
            return SesionVista.empty();
        }

        return users.findById(principal.getId())
                .map(this::session)
                .orElseGet(SesionVista::empty);
    }

    private SesionVista session(Usuario usuario) {
        return new SesionVista(
                new UsuarioActual(
                        usuario.getNombreCompleto(),
                        usuario.getEmail(),
                        usuario.getPlan(),
                        usuario.getAvatarPath(),
                        usuario.getTheme(),
                        usuario.getRol()),
                notifications.unreadContar(usuario),
                notifications.reciente(usuario)
        );
    }
}
