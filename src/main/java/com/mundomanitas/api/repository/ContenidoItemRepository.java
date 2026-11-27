package com.mundomanitas.api.repository;

import com.mundomanitas.api.model.ContenidoItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ContenidoItemRepository extends JpaRepository<ContenidoItem, UUID> {

    @Query("""
            select x from ContenidoItem x where x.moduloId = ?1 order by x.posicion asc, x.id asc
    """)
    List<ContenidoItem> buscarPorModuloIdOrdenarPorPosicionAsc(UUID moduloId);

    @Query("""
            select x from ContenidoItem x where x.origenTipo = ?1 and x.origenId = ?2
    """)
    ContenidoItem buscarPorOrigenTipoAndOrigenId(String origenTipo, UUID origenId);

    @Query("""
            select count(i) from ContenidoItem i, CursoModulo m
            where i.moduloId = m.id and m.cursoId = :cursoId
            """)
    int contarPorCursoId(@Param("cursoId") UUID cursoId);
}
