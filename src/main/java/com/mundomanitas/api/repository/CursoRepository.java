package com.mundomanitas.api.repository;

import com.mundomanitas.api.model.Curso;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Acceso al catálogo guardado en PostgreSQL.
 */
public interface CursoRepository extends JpaRepository<Curso, UUID> {

    @Query("""
            select x from Curso x order by x.posicion asc
    """)
    List<Curso> buscarAllPorOrdenarPorPosicionAsc();

    @Query("""
            select x from Curso x where x.status = ?1 order by x.posicion asc
    """)
    List<Curso> buscarAllPorEstadoOrdenarPorPosicionAsc(String status);

    @Query("""
            select x from Curso x where x.slug = ?1
    """)
    Optional<Curso> buscarPorSlug(String slug);

    @Query("""
            select x from Curso x where x.slug = ?1 and x.status = ?2
    """)
    Optional<Curso> buscarPorSlugAndEstado(String slug, String status);

    @Query("""
            select x from Curso x order by x.posicion asc
    """)
    Page<Curso> buscarAllPorOrdenarPorPosicionAsc(Pageable pageable);

    @Query("""
            select x from Curso x where lower(x.titulo) like lower(concat('%', ?1, '%')) or lower(x.slug) like lower(concat('%', ?2, '%'))
    """)
    Page<Curso> buscarPorTituloIgnorandoMayusculasContieneOrSlugIgnorandoMayusculasContiene( String titulo, String slug, Pageable pageable);

    @Query("""
            select count(x) from Curso x where x.fechaCreacion between ?1 and ?2
    """)
    long contarPorFechaCreacionEntre(OffsetDateTime from, OffsetDateTime to);
}
