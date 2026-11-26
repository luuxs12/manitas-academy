package com.mundomanitas.api.repository;

import com.mundomanitas.api.model.CuestionarioPregunta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface CuestionarioPreguntaRepository extends JpaRepository<CuestionarioPregunta, UUID> {

    @Query("""
            select x from CuestionarioPregunta x where x.ejercicioId = ?1 order by x.posicion asc
    """)
    List<CuestionarioPregunta> buscarPorEjercicioIdOrdenarPorPosicionAsc(UUID ejercicioId);

    @Query("select count(x) from CuestionarioPregunta x where x.ejercicioId = ?1")
    long contarPorEjercicioId(UUID ejercicioId);
}
