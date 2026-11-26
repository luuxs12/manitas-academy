package com.mundomanitas.api.repository;

import com.mundomanitas.api.model.CursoSeccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface CursoSeccionRepository extends JpaRepository<CursoSeccion, UUID> {

    @Query("""
            select x from CursoSeccion x where x.moduloId = ?1 order by x.posicion asc
    """)
    List<CursoSeccion> buscarPorModuloIdOrdenarPorPosicionAsc(UUID moduloId);
}
