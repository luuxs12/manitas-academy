package com.mundomanitas.api.repository;

import com.mundomanitas.api.model.CuestionarioIntento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CuestionarioIntentoRepository extends JpaRepository<CuestionarioIntento, UUID> {

    @Query("""
            select x from CuestionarioIntento x where x.inscripcionId = ?1 and x.ejercicioId = ?2
    """)
    Optional<CuestionarioIntento> buscarPorInscripcionIdAndEjercicioId(UUID inscripcionId, UUID ejercicioId);

    @Query("""
            select case when count(x) > 0 then true else false end from CuestionarioIntento x where x.ejercicioId = ?1
    """)
    boolean existePorEjercicioId(UUID ejercicioId);

    @Query("""
            select x from CuestionarioIntento x where x.inscripcionId in ?1
    """)
    List<CuestionarioIntento> buscarPorInscripcionIdIn(Collection<UUID> inscripcionIds);
}
