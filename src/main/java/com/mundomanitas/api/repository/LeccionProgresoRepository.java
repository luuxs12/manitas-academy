package com.mundomanitas.api.repository;

import com.mundomanitas.api.model.LeccionProgreso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface LeccionProgresoRepository
        extends JpaRepository<LeccionProgreso, LeccionProgreso.Clave> {

    @Query("""
            select x from LeccionProgreso x where x.inscripcionId = ?1 order by x.fechaFinalizacion asc
    """)
    List<LeccionProgreso> buscarPorInscripcionIdOrdenarPorFechaFinalizacionAsc(UUID inscripcionId);

    @Query("""
            select x from LeccionProgreso x where x.inscripcionId in ?1
    """)
    List<LeccionProgreso> buscarPorInscripcionIdIn(Collection<UUID> inscripcionIds);

    @Query("""
            select count(x) from LeccionProgreso x where x.inscripcionId = ?1
    """)
    long contarPorInscripcionId(UUID inscripcionId);

    @Query("""
            select count(x) from LeccionProgreso x where x.fechaFinalizacion between ?1 and ?2
    """)
    long contarPorFechaFinalizacionEntre(OffsetDateTime from, OffsetDateTime to);
}
