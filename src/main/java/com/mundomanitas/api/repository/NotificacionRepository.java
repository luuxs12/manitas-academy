package com.mundomanitas.api.repository;

import com.mundomanitas.api.model.Notificacion;
import com.mundomanitas.api.model.Usuario;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificacionRepository extends JpaRepository<Notificacion, UUID> {

    @Query("""
            select x from Notificacion x where x.usuario = ?1 order by x.fechaCreacion desc
    """)
    List<Notificacion> buscarPorUsuarioOrdenarPorFechaCreacionDesc(Usuario usuario, Pageable pageable);

    @Query("""
            select count(x) from Notificacion x where x.usuario = ?1 and x.fechaLectura is null
    """)
    long contarPorUsuarioAndFechaLecturaEsNulo(Usuario usuario);

    @Query("""
            select x from Notificacion x where x.id = ?1 and x.usuario = ?2
    """)
    Optional<Notificacion> buscarPorIdAndUsuario(UUID id, Usuario usuario);
}
