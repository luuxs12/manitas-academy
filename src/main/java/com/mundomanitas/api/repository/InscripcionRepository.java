package com.mundomanitas.api.repository;

import com.mundomanitas.api.model.Inscripcion;
import com.mundomanitas.api.model.Usuario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;



/**
 * Matrículas de las cuentas.
 *
 * <p>Se cargan junto con el curso para que el panel no haga una consulta
 * extra por cada tarjeta.</p>
 */
public interface InscripcionRepository extends JpaRepository<Inscripcion, UUID> {

    @EntityGraph(attributePaths = "curso")
    @Query("""
            select x from Inscripcion x where x.usuario = ?1 order by x.fechaInscripcion desc
    """)
    List<Inscripcion> buscarPorUsuarioOrdenarPorFechaInscripcionDesc(Usuario usuario);

    @EntityGraph(attributePaths = "curso")
    @Query("""
            select x from Inscripcion x where x.usuario = ?1 and x.curso.slug = ?2
    """)
    Optional<Inscripcion> buscarPorUsuarioAndCursoSlug(Usuario usuario, String slug);

    @Query("""
            select case when count(x) > 0 then true else false end from Inscripcion x where x.usuario.id = ?1 and x.curso.slug = ?2
    """)
    boolean existePorUsuarioIdAndCursoSlug(UUID usuarioId, String slug);

    @EntityGraph(attributePaths = "curso")
    @Query("""
            select x from Inscripcion x where x.usuario.id = ?1 and x.curso.id = ?2
    """)
    Optional<Inscripcion> buscarPorUsuarioIdAndCursoId(UUID usuarioId, UUID cursoId);

    @EntityGraph(attributePaths = "curso")
    @Query("""
            select x from Inscripcion x where x.usuario = ?1 and x.fechaFinalizacion is not null order by x.fechaFinalizacion desc
    """)
    List<Inscripcion> buscarPorUsuarioAndFechaFinalizacionNoEsNuloOrdenarPorFechaFinalizacionDesc(Usuario usuario);

    @Query("""
            select count(x) from Inscripcion x where x.fechaFinalizacion between ?1 and ?2
    """)
    long contarPorFechaFinalizacionEntre(OffsetDateTime from, OffsetDateTime to);

    @Query("""
            select count(x) from Inscripcion x where x.fechaFinalizacion is not null
    """)
    long contarPorFechaFinalizacionNoEsNulo();

    @Query("""
            select count(x) from Inscripcion x where x.curso.id = ?1
    """)
    long contarPorCursoId(UUID cursoId);

    @Query("select e.curso.id as cursoId, count(e) as total from Inscripcion e group by e.curso.id")
    List<Object[]> contarPorCurso();
}
