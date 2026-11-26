package com.mundomanitas.api.service;

import com.mundomanitas.api.actuator.BusinessMetrics;
import com.mundomanitas.api.model.Certificado;
import com.mundomanitas.api.model.Curso;
import com.mundomanitas.api.model.Inscripcion;
import com.mundomanitas.api.model.Usuario;
import com.mundomanitas.api.repository.CertificadoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CertificadoEmisorServiceTest {

    @Mock
    private CertificadoRepository certificates;

    @Mock
    private NotificacionService notifications;

    @Mock
    private BusinessMetrics metrics;

    @InjectMocks
    private CertificadoEmisorServiceImpl emisor;

    @Mock
    private Inscripcion inscripcion;

    @Mock
    private Usuario usuario;

    @Mock
    private Curso curso;

    @Test
    void emiteCertificadoAlCompletarCursoPremium() {
        UUID usuarioId = UUID.randomUUID();
        UUID cursoId = UUID.randomUUID();
        when(inscripcion.getUsuario()).thenReturn(usuario);
        when(usuario.isPremium()).thenReturn(true);
        when(usuario.getId()).thenReturn(usuarioId);
        when(inscripcion.isFinalizado()).thenReturn(true);
        when(inscripcion.getCurso()).thenReturn(curso);
        when(curso.getId()).thenReturn(cursoId);
        when(curso.getSlug()).thenReturn("python-desde-cero");
        when(curso.getTitulo()).thenReturn("Python desde cero");
        when(certificates.buscarPorUsuarioIdAndCursoIdAndFechaRevocacionEsNulo(usuarioId, cursoId))
                .thenReturn(Optional.empty());

        assertTrue(emisor.issueFor(inscripcion));
        verify(certificates).save(any(Certificado.class));
        verify(notifications).notify(any(Usuario.class), anyString(), anyString(), anyString(), anyString());
        verify(metrics).certificadoEmitido();
    }

    @Test
    void noEmiteParaCuentaGratuita() {
        when(inscripcion.getUsuario()).thenReturn(usuario);
        when(usuario.isPremium()).thenReturn(false);

        assertFalse(emisor.issueFor(inscripcion));
        verify(certificates, never()).save(any());
        verify(notifications, never()).notify(any(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void noEmiteDosCertificadosVigentesParaElMismoCurso() {
        UUID usuarioId = UUID.randomUUID();
        UUID cursoId = UUID.randomUUID();
        when(inscripcion.getUsuario()).thenReturn(usuario);
        when(usuario.isPremium()).thenReturn(true);
        when(usuario.getId()).thenReturn(usuarioId);
        when(inscripcion.isFinalizado()).thenReturn(true);
        when(inscripcion.getCurso()).thenReturn(curso);
        when(curso.getId()).thenReturn(cursoId);
        when(certificates.buscarPorUsuarioIdAndCursoIdAndFechaRevocacionEsNulo(usuarioId, cursoId))
                .thenReturn(Optional.of(new Certificado(usuarioId, cursoId, "MM-PYT-12345678")));

        assertFalse(emisor.issueFor(inscripcion));
        verify(certificates, never()).save(any());
    }
}
