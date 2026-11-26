package com.mundomanitas.api.service;

import com.mundomanitas.api.model.Inscripcion;

/**
 * Emisión automática de certificados al completar un curso.
 */
public interface CertificadoEmisorService {

    boolean issueFor(Inscripcion inscripcion);
}
