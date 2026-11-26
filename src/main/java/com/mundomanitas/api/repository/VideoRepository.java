package com.mundomanitas.api.repository;

import com.mundomanitas.api.model.Video;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface VideoRepository extends JpaRepository<Video, UUID> {

    @Query("""
            select x from Video x where x.moduloId = ?1 order by x.posicion asc
    """)
    List<Video> buscarPorModuloIdOrdenarPorPosicionAsc(UUID moduloId);

    /** Evita servir un video de un curso ajeno a la matrícula. */
    @Query("""
            select case when count(v) > 0 then true else false end
            from Video v, CursoModulo m
            where v.id = :videoId and v.moduloId = m.id and m.cursoId = :cursoId
            """)
    boolean existeInCurso(@Param("videoId") UUID videoId, @Param("cursoId") UUID cursoId);

    /** Total real de videos del curso, para calcular el avance. */
    @Query("""
            select count(v) from Video v, CursoModulo m
            where v.moduloId = m.id and m.cursoId = :cursoId
            """)
    int contarPorCursoId(@Param("cursoId") UUID cursoId);

    /** Videos del curso en orden de reproducción, sin depender del número de lección. */
    @Query("""
            select v.id from Video v, CursoModulo m
            where v.moduloId = m.id and m.cursoId = :cursoId
            order by m.posicion, v.posicion, v.titulo
            """)
    List<UUID> buscarIdsPorCursoId(@Param("cursoId") UUID cursoId);

    /** Videos de varios cursos en una sola consulta, para el avance en lote. */
    @Query("""
            select m.cursoId as cursoId, v.id as videoId
            from Video v, CursoModulo m
            where v.moduloId = m.id and m.cursoId in :cursoIds
            """)
    List<CursoVideo> buscarCursoVideos(@Param("cursoIds") Collection<UUID> cursoIds);

    interface CursoVideo {
        UUID getCursoId();

        UUID getVideoId();
    }

    @Query("""
            select v from Video v
            where (:moduloId is null or v.moduloId = :moduloId)
              and (:term is null or lower(v.titulo) like lower(concat('%', cast(:term as string), '%')))
            order by v.posicion, v.titulo
            """)
    Page<Video> search(@Param("moduloId") UUID moduloId, @Param("term") String term, Pageable pageable);
}
