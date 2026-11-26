package com.mundomanitas.api.repository;

import com.mundomanitas.api.model.Ejercicio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface EjercicioRepository extends JpaRepository<Ejercicio, UUID> {

    @Query("""
            select x from Ejercicio x where x.moduloId = ?1 order by x.posicion asc
    """)
    List<Ejercicio> buscarPorModuloIdOrdenarPorPosicionAsc(UUID moduloId);

    @Query("""
            select e from Ejercicio e
            where (:moduloId is null or e.moduloId = :moduloId)
              and (:cursoId is null or exists (
                  select m.id from CursoModulo m where m.id = e.moduloId and m.cursoId = :cursoId
              ))
              and (:term is null or lower(e.titulo) like lower(concat('%', cast(:term as string), '%')))
            order by e.posicion, e.titulo
            """)
    Page<Ejercicio> search(@Param("moduloId") UUID moduloId, @Param("cursoId") UUID cursoId,
                           @Param("term") String term, Pageable pageable);
}
