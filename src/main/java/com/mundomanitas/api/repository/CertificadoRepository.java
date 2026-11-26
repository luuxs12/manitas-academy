package com.mundomanitas.api.repository;

import com.mundomanitas.api.model.Certificado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CertificadoRepository extends JpaRepository<Certificado, UUID> {

    @Query("""
            select x from Certificado x order by x.fechaEmision desc
    """)
    List<Certificado> buscarAllPorOrdenarPorFechaEmisionDesc();

    @Query("""
            select x from Certificado x where x.usuarioId = ?1 and x.cursoId = ?2 and x.fechaRevocacion is null
    """)
    Optional<Certificado> buscarPorUsuarioIdAndCursoIdAndFechaRevocacionEsNulo(UUID usuarioId, UUID cursoId);

    @Query("""
            select count(x) from Certificado x where x.fechaRevocacion is null
    """)
    long contarPorFechaRevocacionEsNulo();

    @Query("""
            select count(x) from Certificado x where x.cursoId = ?1
    """)
    long contarPorCursoId(UUID cursoId);

    @Query("""
            select count(x) from Certificado x where x.fechaEmision between ?1 and ?2
    """)
    long contarPorFechaEmisionEntre(OffsetDateTime from, OffsetDateTime to);

    @Query("""
            select c from Certificado c
            where (:includeRevocado = true or c.fechaRevocacion is null)
              and (:usuarioId is null or c.usuarioId = :usuarioId)
              and (:cursoId is null or c.cursoId = :cursoId)
              and (:term is null or lower(c.code) like lower(concat('%', cast(:term as string), '%')))
            order by c.fechaEmision desc
            """)
    Page<Certificado> search(@Param("includeRevocado") boolean includeRevocado, @Param("usuarioId") UUID usuarioId, @Param("cursoId") UUID cursoId, @Param("term") String term, Pageable pageable);

    /** Certificados vigentes de una cuenta con el curso y los videos reales. */
    @Query("""
            select c.id as certificadoId, c.code as code, c.fechaEmision as fechaEmision,
                   co.titulo as cursoTitulo, co.slug as cursoSlug,
                   (select count(v) from Video v, CursoModulo m
                     where v.moduloId = m.id and m.cursoId = co.id) as videoContar
            from Certificado c, Curso co
            where c.cursoId = co.id and c.usuarioId = :usuarioId and c.fechaRevocacion is null
            order by c.fechaEmision desc
            """)
    List<EmitidoFila> buscarActivoPorUsuario(@Param("usuarioId") UUID usuarioId);

    @Query("""
            select c.id as certificadoId, c.code as code, c.fechaEmision as fechaEmision,
                   co.titulo as cursoTitulo, co.slug as cursoSlug,
                   (select count(v) from Video v, CursoModulo m
                     where v.moduloId = m.id and m.cursoId = co.id) as videoContar
            from Certificado c, Curso co
            where c.cursoId = co.id and c.usuarioId = :usuarioId
              and c.fechaRevocacion is null and co.slug = :cursoSlug
            """)
    Optional<EmitidoFila> buscarActivoPorUsuarioAndCursoSlug(@Param("usuarioId") UUID usuarioId, @Param("cursoSlug") String cursoSlug);

    interface EmitidoFila {
        UUID getCertificadoId();

        String getCode();

        OffsetDateTime getFechaEmision();

        String getCursoTitulo();

        String getCursoSlug();

        int getVideoContar();
    }

    /** Vista previa del admin: incluye revocados y trae los datos reales de persona y curso. */
    @Query("""
            select c.id as certificadoId, c.code as code, c.fechaEmision as fechaEmision,
                   u.nombreCompleto as person, co.titulo as cursoTitulo,
                   (select count(v) from Video v, CursoModulo m
                     where v.moduloId = m.id and m.cursoId = co.id) as videoContar,
                   case when c.fechaRevocacion is null then true else false end as activo
            from Certificado c, Usuario u, Curso co
            where c.id = :id and c.usuarioId = u.id and c.cursoId = co.id
            """)
    Optional<VistapreviaFila> buscarVistapreviaPorId(@Param("id") UUID id);

    interface VistapreviaFila {
        UUID getCertificadoId();

        String getCode();

        OffsetDateTime getFechaEmision();

        String getPerson();

        String getCursoTitulo();

        int getVideoContar();

        boolean isActivo();
    }
}
