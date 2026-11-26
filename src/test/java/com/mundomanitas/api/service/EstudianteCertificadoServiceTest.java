package com.mundomanitas.api.service;

import com.mundomanitas.api.model.Usuario;
import com.mundomanitas.api.repository.CertificadoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Certificados del estudiante")
class EstudianteCertificadoServiceTest {

    @Mock private CertificadoRepository certificates;
    @Mock private Usuario estudiante;

    private EstudianteCertificadoService service;
    private UUID usuarioId;

    @BeforeEach
    void setUp() {
        service = new EstudianteCertificadoServiceImpl(certificates);
        usuarioId = UUID.randomUUID();
        lenient().when(estudiante.getId()).thenReturn(usuarioId);
    }

    @Test
    @DisplayName("el listado usa el código, la fecha y los videos reales del certificado")
    void disponibleUsesEmitidoDatos() {
        UUID certificadoId = UUID.randomUUID();
        OffsetDateTime fechaEmision = OffsetDateTime.parse("2026-11-20T10:15:00Z");
        CertificadoRepository.EmitidoFila row = emitidoFila(
                certificadoId, "MM-RED-ABCD1234", fechaEmision, "Redes", "redes", 3);
        when(certificates.buscarActivoPorUsuario(usuarioId)).thenReturn(List.of(row));

        List<EstudianteCertificadoService.Emitido> resultado = service.disponible(estudiante);

        assertThat(resultado).containsExactly(new EstudianteCertificadoService.Emitido(
                certificadoId, "MM-RED-ABCD1234", fechaEmision, "Redes", "redes", 3));
        verify(certificates).buscarActivoPorUsuario(usuarioId);
    }

    @Test
    @DisplayName("la ficha busca el certificado vigente del curso por slug")
    void buscarActivoUsesCursoSlug() {
        UUID certificadoId = UUID.randomUUID();
        OffsetDateTime fechaEmision = OffsetDateTime.parse("2026-11-21T10:15:00Z");
        CertificadoRepository.EmitidoFila row = emitidoFila(
                certificadoId, "MM-PYT-1234ABCD", fechaEmision, "Python", "python", 5);
        when(certificates.buscarActivoPorUsuarioAndCursoSlug(usuarioId, "python"))
                .thenReturn(Optional.of(row));

        Optional<EstudianteCertificadoService.Emitido> resultado = service.buscarActivo(estudiante, "python");

        assertThat(resultado).contains(new EstudianteCertificadoService.Emitido(
                certificadoId, "MM-PYT-1234ABCD", fechaEmision, "Python", "python", 5));
        verify(certificates).buscarActivoPorUsuarioAndCursoSlug(usuarioId, "python");
    }

    @Test
    @DisplayName("un certificado revocado no abre la ficha")
    void revocadoCertificadoCannotBeOpened() {
        when(certificates.buscarActivoPorUsuarioAndCursoSlug(usuarioId, "python"))
                .thenReturn(Optional.empty());

        assertThat(service.buscarActivo(estudiante, "python")).isEmpty();
    }

    private CertificadoRepository.EmitidoFila emitidoFila(UUID certificadoId, String code,
                                                      OffsetDateTime fechaEmision, String titulo,
                                                      String slug, int videoContar) {
        CertificadoRepository.EmitidoFila row = mock(CertificadoRepository.EmitidoFila.class);
        when(row.getCertificadoId()).thenReturn(certificadoId);
        when(row.getCode()).thenReturn(code);
        when(row.getFechaEmision()).thenReturn(fechaEmision);
        when(row.getCursoTitulo()).thenReturn(titulo);
        when(row.getCursoSlug()).thenReturn(slug);
        when(row.getVideoContar()).thenReturn(videoContar);
        return row;
    }
}
