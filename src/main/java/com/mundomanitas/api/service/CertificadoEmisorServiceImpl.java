package com.mundomanitas.api.service;

import com.mundomanitas.api.model.Certificado;
import com.mundomanitas.api.actuator.BusinessMetrics;
import com.mundomanitas.api.model.Inscripcion;
import com.mundomanitas.api.model.Usuario;
import com.mundomanitas.api.repository.CertificadoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Emisión automática de certificados.
 *
 * <p>Se llama cuando una matrícula llega al 100%. Solo emite para cuentas
 * Premium, una vez por curso y sin sobrescribir un certificado revocado.</p>
 */
@Service
public class CertificadoEmisorServiceImpl implements CertificadoEmisorService {

    private static final Logger log = LoggerFactory.getLogger(CertificadoEmisorServiceImpl.class);

    private final CertificadoRepository certificates;
    private final NotificacionService notifications;
    private final BusinessMetrics metrics;

    public CertificadoEmisorServiceImpl(CertificadoRepository certificates,
                                        NotificacionService notifications,
                                        BusinessMetrics metrics) {
        this.certificates = certificates;
        this.notifications = notifications;
        this.metrics = metrics;
    }

    /** Devuelve true cuando se emitió un certificado nuevo. */
    @Override
    @Transactional
    public boolean issueFor(Inscripcion inscripcion) {
        Usuario usuario = inscripcion.getUsuario();
        if (usuario == null || !usuario.isPremium() || !inscripcion.isFinalizado()) {
            return false;
        }
        if (certificates.buscarPorUsuarioIdAndCursoIdAndFechaRevocacionEsNulo(
                usuario.getId(), inscripcion.getCurso().getId()).isPresent()) {
            return false;
        }

        String slug = inscripcion.getCurso().getSlug();
        String prefix = slug.substring(0, Math.min(3, slug.length())).toUpperCase();
        String code = "MM-" + prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        certificates.save(new Certificado(usuario.getId(), inscripcion.getCurso().getId(), code));
        metrics.certificadoEmitido();
        notifications.notify(
                usuario,
                NotificacionService.CERTIFICATE,
                "Certificado emitido",
                "Ya tenés el certificado de «" + inscripcion.getCurso().getTitulo() + "».",
                "/panel/certificados"
        );
        log.info("Certificado emitido para usuario {} en curso {}",
                usuario.getId(), inscripcion.getCurso().getId());
        return true;
    }
}
