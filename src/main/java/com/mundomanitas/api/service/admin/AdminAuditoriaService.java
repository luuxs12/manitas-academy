package com.mundomanitas.api.service.admin;

import com.mundomanitas.api.dto.admin.AdminEvento;
import com.mundomanitas.api.model.AuditoriaRegistro;
import com.mundomanitas.api.model.Usuario;
import com.mundomanitas.api.repository.AuditoriaRegistroRepository;
import com.mundomanitas.api.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Escribe y consulta la auditoría de acciones administrativas. */
@Service
public class AdminAuditoriaService {

    private final AuditoriaRegistroRepository logs;
    private final UsuarioRepository users;
    private final AdminFormato format;

    public AdminAuditoriaService(AuditoriaRegistroRepository logs, UsuarioRepository users, AdminFormato format) {
        this.logs = logs;
        this.users = users;
        this.format = format;
    }

    @Transactional
    public void record(UUID actor, String action, String entity, UUID entityId, String detail) {
        logs.save(new AuditoriaRegistro(actor, action, entity, entityId, detail));
    }

    @Transactional(readOnly = true)
    public Page<AdminEvento> page(Pageable pageable) {
        Map<UUID, String> names = new HashMap<>();
        for (Usuario usuario : users.findAll()) {
            names.put(usuario.getId(), usuario.getNombreCompleto());
        }
        return logs.buscarAllPorOrdenarPorFechaCreacionDesc(pageable).map(log ->
                new AdminEvento(
                        log.getId().toString(),
                        log.getAction(),
                        names.getOrDefault(log.getActorId(), "Usuario eliminado"),
                        log.getEntity() + ": " + log.getDetail(),
                        format.completo(log.getFechaCreacion())));
    }
}
