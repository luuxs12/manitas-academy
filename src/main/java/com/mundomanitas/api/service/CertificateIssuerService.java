package com.mundomanitas.api.service;

import com.mundomanitas.api.model.Enrollment;

/**
 * Emisión automática de certificados al completar un curso.
 */
public interface CertificateIssuerService {

    boolean issueFor(Enrollment enrollment);
}
