package com.mundomanitas.api.service;

import com.mundomanitas.api.dto.UsuarioActual;
import com.mundomanitas.api.model.Notificacion;
import org.springframework.security.core.Authentication;

import java.util.List;

/**
 * Carga los datos de la persona autenticada que necesitan las vistas.
 */
public interface ActualUsuarioService {

    SesionVista load(Authentication authentication);

    record SesionVista(UsuarioActual usuarioActual, long unreadContar,
                       List<Notificacion> recienteNotifications) {

        public static SesionVista empty() {
            return new SesionVista(null, 0, List.of());
        }
    }
}
