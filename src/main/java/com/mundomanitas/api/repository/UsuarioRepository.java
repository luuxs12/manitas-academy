package com.mundomanitas.api.repository;

import com.mundomanitas.api.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Consultas de cuentas. El email se guarda en minúsculas, pero se busca
 * sin distinguir mayúsculas para no duplicar personas por una mayúscula.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    @Query("""
            select x from Usuario x where lower(x.email) = lower(?1)
    """)
    Optional<Usuario> buscarPorEmailIgnorandoMayusculas(String email);

    @Query("""
            select case when count(x) > 0 then true else false end from Usuario x where lower(x.email) = lower(?1)
    """)
    boolean existePorEmailIgnorandoMayusculas(String email);

    @Query("""
            select u from Usuario u
            where (:rol is null or u.rol = :rol)
              and (:term is null or lower(u.nombreCompleto) like lower(concat('%', cast(:term as string), '%'))
                              or lower(u.email) like lower(concat('%', cast(:term as string), '%')))
            order by u.fechaCreacion desc
            """)
    Page<Usuario> search(@Param("rol") String rol, @Param("term") String term, Pageable pageable);

    @Query("""
            select x from Usuario x order by x.fechaCreacion desc limit 5
    """)
    List<Usuario> buscarTop5PorOrdenarPorFechaCreacionDesc();

    @Query("""
            select count(x) from Usuario x where x.fechaCreacion between ?1 and ?2
    """)
    long contarPorFechaCreacionEntre(OffsetDateTime from, OffsetDateTime to);
}
