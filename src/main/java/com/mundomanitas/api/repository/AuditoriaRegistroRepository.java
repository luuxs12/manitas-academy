package com.mundomanitas.api.repository;

import com.mundomanitas.api.model.AuditoriaRegistro;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface AuditoriaRegistroRepository extends JpaRepository<AuditoriaRegistro, UUID> {

    @Query("""
            select x from AuditoriaRegistro x order by x.fechaCreacion desc
    """)
    Page<AuditoriaRegistro> buscarAllPorOrdenarPorFechaCreacionDesc(Pageable pageable);

    @Query("""
            select x from AuditoriaRegistro x order by x.fechaCreacion desc
    """)
    List<AuditoriaRegistro> buscarAllPorOrdenarPorFechaCreacionDesc();

    @Query("""
            select x from AuditoriaRegistro x where x.fechaCreacion > ?1 order by x.fechaCreacion desc limit 8
    """)
    List<AuditoriaRegistro> buscarTop8PorFechaCreacionDespuesOrdenarPorFechaCreacionDesc(OffsetDateTime since);

    @Query("""
            select count(x) from AuditoriaRegistro x where x.fechaCreacion > ?1
    """)
    long contarPorFechaCreacionDespues(OffsetDateTime since);
}
