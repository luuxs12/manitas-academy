package com.mundomanitas.api.service;

import com.mundomanitas.api.model.Notificacion;
import com.mundomanitas.api.model.Usuario;
import com.mundomanitas.api.repository.NotificacionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Avisos que se escriben cuando ocurre algo real en la cuenta. */
@Service
public class NotificacionService {

    public static final String ENROLLMENT = "ENROLLMENT";
    public static final String COMPLETION = "COMPLETION";
    public static final String PAYMENT = "PAYMENT";
    public static final String CERTIFICATE = "CERTIFICATE";
    public static final String SYSTEM = "SYSTEM";

    private final NotificacionRepository notifications;

    public NotificacionService(NotificacionRepository notifications) {
        this.notifications = notifications;
    }

    @Transactional
    public void notify(Usuario usuario, String kind, String titulo, String message, String targetUrl) {
        notifications.save(new Notificacion(usuario, kind, titulo, message, targetUrl));
    }

    @Transactional(readOnly = true)
    public List<Notificacion> reciente(Usuario usuario) {
        return notifications.buscarPorUsuarioOrdenarPorFechaCreacionDesc(usuario, PageRequest.of(0, 20));
    }

    @Transactional(readOnly = true)
    public long unreadContar(Usuario usuario) {
        return notifications.contarPorUsuarioAndFechaLecturaEsNulo(usuario);
    }

    @Transactional
    public void markLeido(Usuario usuario, UUID id) {
        notifications.buscarPorIdAndUsuario(id, usuario).ifPresent(Notificacion::markLeido);
    }

    @Transactional
    public int markAllLeido(Usuario usuario) {
        List<Notificacion> pending = notifications.buscarPorUsuarioOrdenarPorFechaCreacionDesc(
                usuario, PageRequest.of(0, 50));
        int changed = 0;
        for (Notificacion notificacion : pending) {
            if (!notificacion.isLeido()) {
                notificacion.markLeido();
                changed++;
            }
        }
        return changed;
    }
}
