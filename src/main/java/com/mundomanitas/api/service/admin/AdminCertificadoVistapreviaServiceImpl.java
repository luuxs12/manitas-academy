package com.mundomanitas.api.service.admin;

import com.mundomanitas.api.repository.CertificadoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/** Implementación de la vista previa administrativa de certificados. */
@Service
public class AdminCertificadoVistapreviaServiceImpl implements AdminCertificadoVistapreviaService {

    private static final Logger log = LoggerFactory.getLogger(AdminCertificadoVistapreviaServiceImpl.class);

    private final CertificadoRepository certificates;

    public AdminCertificadoVistapreviaServiceImpl(CertificadoRepository certificates) {
        this.certificates = certificates;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Vistaprevia> vistaprevia(UUID id) {
        Optional<Vistaprevia> resultado = certificates.buscarVistapreviaPorId(id).map(row -> new Vistaprevia(
                row.getCertificadoId(),
                row.getCode(),
                row.getFechaEmision(),
                row.getPerson(),
                row.getCursoTitulo(),
                row.getVideoContar(),
                row.isActivo()));
        log.info("Vista previa de certificado {} resuelta: {}", id, resultado.isPresent());
        return resultado;
    }
}
