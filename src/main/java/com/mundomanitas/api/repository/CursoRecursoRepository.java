package com.mundomanitas.api.repository;

import com.mundomanitas.api.model.CursoRecurso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface CursoRecursoRepository extends JpaRepository<CursoRecurso, UUID> {

    @Query("""
            select x from CursoRecurso x where x.cursoId = ?1 order by x.posicion asc
    """)
    List<CursoRecurso> buscarPorCursoIdOrdenarPorPosicionAsc(UUID cursoId);
}
