package com.mundomanitas.api.service.admin;

import com.mundomanitas.api.dto.admin.AdminCertificadoFormulario;
import com.mundomanitas.api.dto.admin.FilaCertificado;
import com.mundomanitas.api.dto.admin.CertificadoElegible;
import com.mundomanitas.api.model.Certificado;
import com.mundomanitas.api.model.Curso;
import com.mundomanitas.api.model.Inscripcion;
import com.mundomanitas.api.model.Usuario;
import com.mundomanitas.api.repository.CertificadoRepository;
import com.mundomanitas.api.repository.CursoRepository;
import com.mundomanitas.api.repository.InscripcionRepository;
import com.mundomanitas.api.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Emisión y revocación de certificados, sujeta a las reglas de negocio. */
@Service
public class AdminCertificadoService {

    private final CertificadoRepository certificates;
    private final UsuarioRepository users;
    private final CursoRepository courses;
    private final InscripcionRepository enrollments;
    private final AdminAuditoriaService auditoria;
    private final AdminFormato format;

    public AdminCertificadoService(
            CertificadoRepository certificates,
            UsuarioRepository users,
            CursoRepository courses,
            InscripcionRepository enrollments,
            AdminAuditoriaService auditoria,
            AdminFormato format
    ) {
        this.certificates = certificates;
        this.users = users;
        this.courses = courses;
        this.enrollments = enrollments;
        this.auditoria = auditoria;
        this.format = format;
    }

    @Transactional(readOnly = true)
    public Page<FilaCertificado> certificates(boolean includeRevocado, UUID usuarioId,
                                             UUID cursoId, String term, Pageable pageable) {
        String clean = term == null || term.isBlank() ? null : term.trim();
        Map<UUID, String> usuarioNames = usuarioNames();
        Map<UUID, Curso> cursoMap = courses.findAll().stream()
                .collect(Collectors.toMap(Curso::getId, Function.identity()));
        return certificates.search(includeRevocado, usuarioId, cursoId, clean, pageable)
                .map(certificado -> new FilaCertificado(
                        certificado.getId(),
                        certificado.getCode(),
                        usuarioNames.getOrDefault(certificado.getUsuarioId(), "Usuario eliminado"),
                        cursoMap.containsKey(certificado.getCursoId())
                                ? cursoMap.get(certificado.getCursoId()).getTitulo()
                                : "Curso eliminado",
                        format.completo(certificado.getFechaEmision()),
                        certificado.isActivo()));
    }

    @Transactional(readOnly = true)
    public List<CertificadoElegible> elegible() {
        Map<UUID, Curso> cursoMap = courses.findAll().stream()
                .collect(Collectors.toMap(Curso::getId, Function.identity()));
        return enrollments.findAll().stream()
                .filter(Inscripcion::isFinalizado)
                .filter(inscripcion -> inscripcion.getUsuario().isPremium())
                .filter(inscripcion -> certificates.buscarPorUsuarioIdAndCursoIdAndFechaRevocacionEsNulo(
                        inscripcion.getUsuario().getId(), inscripcion.getCurso().getId()).isEmpty())
                .map(inscripcion -> {
                    Curso curso = cursoMap.get(inscripcion.getCurso().getId());
                    return new CertificadoElegible(
                            inscripcion.getUsuario().getId(),
                            inscripcion.getUsuario().getNombreCompleto(),
                            inscripcion.getCurso().getId(),
                            curso == null ? inscripcion.getCurso().getTitulo() : curso.getTitulo());
                })
                .distinct()
                .toList();
    }

    @Transactional
    public String issue(AdminCertificadoFormulario formulario, UUID actor) {
        Usuario usuario = users.findById(formulario.usuarioId())
                .orElseThrow(() -> new IllegalArgumentException("No existe esa cuenta."));
        Curso curso = courses.findById(formulario.cursoId())
                .orElseThrow(() -> new IllegalArgumentException("No existe ese curso."));
        if (!usuario.isPremium()) {
            throw new IllegalArgumentException(
                    "Los certificados están incluidos en los planes Premium.");
        }
        Inscripcion inscripcion = enrollments.buscarPorUsuarioIdAndCursoId(usuario.getId(), curso.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "La persona no está inscripta a ese curso."));
        if (!inscripcion.isFinalizado()) {
            throw new IllegalArgumentException("El curso todavía no fue finalizado.");
        }
        certificates.buscarPorUsuarioIdAndCursoIdAndFechaRevocacionEsNulo(usuario.getId(), curso.getId())
                .ifPresent(certificado -> {
                    throw new IllegalArgumentException("Ese certificado ya está emitido.");
                });
        String code = "MM-" + curso.getSlug().substring(0, Math.min(3, curso.getSlug().length())).toUpperCase()
                + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Certificado certificado = new Certificado(usuario.getId(), curso.getId(), code);
        certificates.save(certificado);
        auditoria.record(actor, "ISSUE", "CERTIFICADO", certificado.getId(), code);
        return code;
    }

    @Transactional
    public void revoke(UUID id, UUID actor) {
        Certificado certificado = certificates.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe ese certificado."));
        certificado.revoke();
        auditoria.record(actor, "REVOKE", "CERTIFICADO", id, certificado.getCode());
    }

    private Map<UUID, String> usuarioNames() {
        return users.findAll().stream().collect(Collectors.toMap(
                Usuario::getId, Usuario::getNombreCompleto, (a, b) -> a, java.util.LinkedHashMap::new));
    }
}
