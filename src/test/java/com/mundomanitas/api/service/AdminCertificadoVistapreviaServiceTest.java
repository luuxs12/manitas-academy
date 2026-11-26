package com.mundomanitas.api.service;

import com.mundomanitas.api.service.admin.AdminCertificadoVistapreviaService;
import com.mundomanitas.api.service.admin.AdminCertificadoVistapreviaServiceImpl;
import com.mundomanitas.api.repository.CertificadoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Vista previa administrativa de certificados")
class AdminCertificadoVistapreviaServiceTest {

    @Mock private CertificadoRepository certificates;

    private AdminCertificadoVistapreviaService service;

    @BeforeEach
    void setUp() {
        service = new AdminCertificadoVistapreviaServiceImpl(certificates);
    }

    @Test
    @DisplayName("la vista previa trae persona, curso, videos y estado reales, incluso si está revocada")
    void vistapreviaIncludesRealDatosAndState() {
        UUID id = UUID.randomUUID();
        OffsetDateTime fechaEmision = OffsetDateTime.parse("2026-11-20T10:15:00Z");
        CertificadoRepository.VistapreviaFila row = vistapreviaFila(
                id, "MM-RED-ABCD1234", fechaEmision, "Ana Quispe", "Redes", 3, false);
        when(certificates.buscarVistapreviaPorId(id)).thenReturn(Optional.of(row));

        Optional<AdminCertificadoVistapreviaService.Vistaprevia> resultado = service.vistaprevia(id);

        assertThat(resultado).contains(new AdminCertificadoVistapreviaService.Vistaprevia(
                id, "MM-RED-ABCD1234", fechaEmision, "Ana Quispe", "Redes", 3, false));
        verify(certificates).buscarVistapreviaPorId(id);
    }

    @Test
    @DisplayName("un id que no existe no abre ninguna vista previa")
    void missingCertificadoHasNoVistaprevia() {
        UUID id = UUID.randomUUID();
        when(certificates.buscarVistapreviaPorId(id)).thenReturn(Optional.empty());

        assertThat(service.vistaprevia(id)).isEmpty();
    }

    private CertificadoRepository.VistapreviaFila vistapreviaFila(UUID id, String code,
                                                        OffsetDateTime fechaEmision, String person,
                                                        String cursoTitulo, int videoContar,
                                                        boolean activo) {
        CertificadoRepository.VistapreviaFila row = mock(CertificadoRepository.VistapreviaFila.class);
        when(row.getCertificadoId()).thenReturn(id);
        when(row.getCode()).thenReturn(code);
        when(row.getFechaEmision()).thenReturn(fechaEmision);
        when(row.getPerson()).thenReturn(person);
        when(row.getCursoTitulo()).thenReturn(cursoTitulo);
        when(row.getVideoContar()).thenReturn(videoContar);
        when(row.isActivo()).thenReturn(activo);
        return row;
    }
}
