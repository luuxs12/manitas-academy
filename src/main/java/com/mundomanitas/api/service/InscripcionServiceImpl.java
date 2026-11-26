package com.mundomanitas.api.service;

import com.mundomanitas.api.actuator.BusinessMetrics;
import com.mundomanitas.api.exception.PremiumRequeridoException;
import com.mundomanitas.api.model.Curso;
import com.mundomanitas.api.model.Inscripcion;
import com.mundomanitas.api.model.Usuario;
import com.mundomanitas.api.repository.InscripcionRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Reglas de inscripción y avance de los cursos de una cuenta.
 *
 * <p>Nadie nace inscripto: cada matrícula se crea con una acción explícita.
 * Los cursos de paga requieren plan Premium y la fecha de finalización
 * habilita el certificado. Cada acción genera un aviso real.</p>
 */
@Service
public class InscripcionServiceImpl implements InscripcionService {

    private static final Logger log = LoggerFactory.getLogger(InscripcionServiceImpl.class);

    private final InscripcionRepository enrollments;
    private final CursoCatalog catalog;
    private final LeccionProgresoService lessons;
    private final NotificacionService notifications;
    private final CertificadoEmisorService certificates;
    private final BusinessMetrics metrics;

    public InscripcionServiceImpl(
            InscripcionRepository enrollments,
            CursoCatalog catalog,
            LeccionProgresoService lessons,
            NotificacionService notifications,
            CertificadoEmisorService certificates,
            BusinessMetrics metrics
    ) {
        this.enrollments = enrollments;
        this.catalog = catalog;
        this.lessons = lessons;
        this.notifications = notifications;
        this.certificates = certificates;
        this.metrics = metrics;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Inscripcion> mine(Usuario usuario) {
        return enrollments.buscarPorUsuarioOrdenarPorFechaInscripcionDesc(usuario).stream()
                .filter(InscripcionServiceImpl::isPublished)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Inscripcion> finalizado(Usuario usuario) {
        return enrollments.buscarPorUsuarioAndFechaFinalizacionNoEsNuloOrdenarPorFechaFinalizacionDesc(usuario).stream()
                .filter(InscripcionServiceImpl::isPublished)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isInscripto(UUID usuarioId, String slug) {
        checkNotNull(usuarioId, "userId");
        return enrollments.existePorUsuarioIdAndCursoSlug(usuarioId, requeridoSlug(slug));
    }

    @Override
    @Transactional(readOnly = true)
    public Inscripcion buscarMine(Usuario usuario, String slug) {
        checkNotNull(usuario, "user");
        Inscripcion inscripcion = enrollments.buscarPorUsuarioAndCursoSlug(usuario, requeridoSlug(slug))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No estás inscripto en ese curso"));
        if (!isPublished(inscripcion)) {
            log.info("Acceso a curso en borrador denegado usuario={} curso={}",
                    usuario.getId(), slug);
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "El curso no está disponible");
        }
        return inscripcion;
    }

    /**
     * Inscribe a la persona en un curso. Repetir la acción no crea otra
     * matrícula: devuelve la que ya existe.
     */
    @Override
    @Transactional
    public Inscripcion enroll(Usuario usuario, String slug) {
        checkNotNull(usuario, "user");
        String cleanSlug = requeridoSlug(slug);
        Curso curso = catalog.buscarPorSlug(cleanSlug)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Curso no encontrado"));

        if (!curso.isFree() && !usuario.isPremium()) {
            throw new PremiumRequeridoException(curso.getTitulo());
        }

        var existing = enrollments.buscarPorUsuarioAndCursoSlug(usuario, cleanSlug);
        if (existing.isPresent()) {
            return existing.get();
        }

        Inscripcion inscripcion = enrollments.save(new Inscripcion(usuario, curso));
        metrics.inscripcionCreado();
        notifications.notify(
                usuario,
                NotificacionService.ENROLLMENT,
                "Te inscribiste en un curso",
                "«" + curso.getTitulo() + "» ya está en Mis cursos.",
                "/panel/cursos"
        );
        log.info("Inscripción creada usuario={} curso={}", usuario.getId(), cleanSlug);
        return inscripcion;
    }

    /** Marca como terminado un curso en el que la persona está inscripta. */
    @Override
    @Transactional
    public Inscripcion complete(Usuario usuario, String slug) {
        Inscripcion inscripcion = buscarMine(usuario, slug);
        lessons.completeAll(inscripcion);
        if (!inscripcion.isFinalizado()) {
            inscripcion.complete();
            enrollments.save(inscripcion);
            notifications.notify(
                    usuario,
                    NotificacionService.COMPLETION,
                    "Terminaste un curso",
                    "«" + inscripcion.getCurso().getTitulo() + "» quedó marcado como terminado.",
                    "/panel/certificados"
            );
            certificates.issueFor(inscripcion);
            log.info("Curso completado usuario={} curso={}", usuario.getId(), slug);
        }
        return inscripcion;
    }

    @Override
    @Transactional
    public Inscripcion toggleVideo(Usuario usuario, String slug, UUID videoId) {
        Inscripcion inscripcion = buscarMine(usuario, slug);
        boolean justFinished = lessons.toggle(usuario, slug, videoId);
        if (justFinished && !inscripcion.isFinalizado()) {
            inscripcion.complete();
            enrollments.save(inscripcion);
            notifications.notify(
                    usuario,
                    NotificacionService.COMPLETION,
                    "Terminaste un curso",
                    "Completaste todas las lecciones de «"
                            + inscripcion.getCurso().getTitulo() + "».",
                    "/panel/certificados"
            );
            certificates.issueFor(inscripcion);
            log.info("Curso completado por videos usuario={} curso={}",
                    usuario.getId(), slug);
        }
        return inscripcion;
    }

    /** Un curso en borrador no se sirve al alumnado aunque exista la matrícula. */
    private static boolean isPublished(Inscripcion inscripcion) {
        return inscripcion.getCurso() != null
                && "PUBLICADO".equals(inscripcion.getCurso().getEstado());
    }

    /** Primer curso inscripto todavía sin terminar, para "Continuar". */
    @Override
    @Transactional(readOnly = true)
    public Inscripcion next(Usuario usuario) {
        return mine(usuario).stream()
                .filter(inscripcion -> !inscripcion.isFinalizado())
                .findFirst()
                .orElse(null);
    }

    private static String requeridoSlug(String slug) {
        String clean = StringUtils.trimToNull(slug);
        if (clean == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Curso no encontrado");
        }
        return clean;
    }
}
