package com.mundomanitas.api.repository;

import com.mundomanitas.api.model.ItemProgreso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ItemProgresoRepository
        extends JpaRepository<ItemProgreso, ItemProgreso.Clave> {

    @Query("""
            select x from ItemProgreso x where x.inscripcionId = ?1 order by x.fechaFinalizacion asc
    """)
    List<ItemProgreso> buscarPorInscripcionIdOrdenarPorFechaFinalizacionAsc(UUID inscripcionId);

    @Query("""
            select count(x) from ItemProgreso x where x.inscripcionId = ?1
    """)
    long contarPorInscripcionId(UUID inscripcionId);
}
