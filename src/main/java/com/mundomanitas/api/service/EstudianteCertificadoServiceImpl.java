package com.mundomanitas.api.service;

import com.mundomanitas.api.model.Usuario;
import com.mundomanitas.api.repository.CertificadoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** Implementación de los certificados visibles para el estudiante. */
@Service
public class EstudianteCertificadoServiceImpl implements EstudianteCertificadoService {

    private static final Logger log = LoggerFactory.getLogger(EstudianteCertificadoServiceImpl.class);

    private final CertificadoRepository certificates;

    public EstudianteCertificadoServiceImpl(CertificadoRepository certificates) {
        this.certificates = certificates;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Emitido> disponible(Usuario usuario) {
        List<Emitido> resultado = certificates.buscarActivoPorUsuario(usuario.getId()).stream()
                .map(this::view)
                .toList();
        log.info("Certificados vigentes recuperados para usuario {}: {}", usuario.getId(), resultado.size());
        return resultado;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Emitido> buscarActivo(Usuario usuario, String cursoSlug) {
        return certificates.buscarActivoPorUsuarioAndCursoSlug(usuario.getId(), cursoSlug)
                .map(this::view);
    }

    private Emitido view(CertificadoRepository.EmitidoFila row) {
        return new Emitido(
                row.getCertificadoId(),
                row.getCode(),
                row.getFechaEmision(),
                row.getCursoTitulo(),
                row.getCursoSlug(),
                row.getVideoContar());
    }
}
