package com.mundomanitas.api.repository;

import com.mundomanitas.api.model.CursoModulo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CursoModuloRepository extends JpaRepository<CursoModulo, UUID> {

    @Query("""
            select x from CursoModulo x order by x.posicion asc
    """)
    List<CursoModulo> buscarAllPorOrdenarPorPosicionAsc();

    @Query("""
            select x from CursoModulo x where x.cursoId = ?1 order by x.posicion asc
    """)
    List<CursoModulo> buscarPorCursoIdOrdenarPorPosicionAsc(UUID cursoId);

    @Query("""
            select m from CursoModulo m
            where (:cursoId is null or m.cursoId = :cursoId)
              and (:term is null or lower(m.titulo) like lower(concat('%', cast(:term as string), '%')))
            order by m.posicion, m.titulo
            """)
    Page<CursoModulo> search(@Param("cursoId") UUID cursoId, @Param("term") String term, Pageable pageable);
}
