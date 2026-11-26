package com.mundomanitas.api.service.admin;

import com.mundomanitas.api.dto.admin.ResultadoExportacion;
import com.mundomanitas.api.model.ContenidoItem;
import com.mundomanitas.api.model.Curso;
import com.mundomanitas.api.model.CursoModulo;
import com.mundomanitas.api.model.CursoSeccion;
import com.mundomanitas.api.repository.ContenidoItemRepository;
import com.mundomanitas.api.repository.CursoModuloRepository;
import com.mundomanitas.api.repository.CursoRepository;
import com.mundomanitas.api.repository.CursoSeccionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PDF de una sesión del curso")
class AdminSesionPdfServiceTest {

    @Mock private CursoRepository courses;
    @Mock private CursoModuloRepository modules;
    @Mock private CursoSeccionRepository sections;
    @Mock private ContenidoItemRepository items;
    @Mock private AdminAuditoriaService auditoria;

    private AdminSesionPdfService service;

    @BeforeEach
    void setUp() {
        service = new AdminSesionPdfServiceImpl(courses, modules, sections, items,
                auditoria, new SimplePdfEscritor());
    }

    @Test
    @DisplayName("el PDF reúne el curso, la sesión y los ítems reales, incluso los ocultos")
    void sesionPdfUsesRealContenido() {
        UUID actor = UUID.randomUUID();
        UUID cursoId = UUID.randomUUID();
        UUID moduloId = UUID.randomUUID();

        Curso curso = mock(Curso.class);
        when(curso.getTitulo()).thenReturn("Python");
        when(courses.findById(cursoId)).thenReturn(Optional.of(curso));

        CursoModulo modulo = mock(CursoModulo.class);
        when(modulo.getCursoId()).thenReturn(cursoId);
        when(modulo.getTitulo()).thenReturn("Sesion 01");
        when(modules.findById(moduloId)).thenReturn(Optional.of(modulo));

        UUID seccionId = UUID.randomUUID();
        CursoSeccion seccion = mock(CursoSeccion.class);
        when(seccion.getId()).thenReturn(seccionId);
        when(seccion.getTitulo()).thenReturn("Temas de la sesion");
        when(sections.buscarPorModuloIdOrdenarPorPosicionAsc(moduloId)).thenReturn(List.of(seccion));

        ContenidoItem video = item("Video 1", "VIDEO", true, seccionId);
        ContenidoItem hidden = item("Guia 2", "MATERIAL", false, seccionId);
        ContenidoItem loose = item("Ejercicio 3", "EJERCICIO", true, null);
        when(items.buscarPorModuloIdOrdenarPorPosicionAsc(moduloId))
                .thenReturn(List.of(video, hidden, loose));

        ResultadoExportacion resultado = service.moduloPdf(moduloId, actor);

        String pdf = new String(resultado.bytes(), StandardCharsets.ISO_8859_1);
        assertThat(resultado.contenidoTipo()).isEqualTo("application/pdf");
        assertThat(resultado.nombreArchivo()).startsWith("mundo-manitas-sesion-").endsWith(".pdf");
        assertThat(pdf).startsWith("%PDF-1.4")
                .contains("/Im0 Do")
                .contains("Sesión: Sesion 01")
                .contains("Python")
                .contains("Temas de la sesion")
                .contains("Video 1")
                .contains("Guia 2")
                .contains("Sin sección")
                .contains("Ítems:")
                .contains("Visible")
                .contains("Oculta")
                .contains("Página 1 / 1");
        verify(auditoria).record(actor, "EXPORT", "MODULO", moduloId, "Sesion 01");
    }

    @Test
    @DisplayName("un módulo inexistente no genera archivo ni auditoría")
    void missingModuloIsRejected() {
        UUID moduloId = UUID.randomUUID();
        when(modules.findById(moduloId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.moduloPdf(moduloId, UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("El módulo solicitado no existe.");

        verifyNoInteractions(auditoria);
    }

    private ContenidoItem item(String titulo, String type, boolean visible, UUID seccionId) {
        ContenidoItem item = mock(ContenidoItem.class);
        when(item.getTitulo()).thenReturn(titulo);
        when(item.getOrigenTipo()).thenReturn(type);
        if (seccionId != null) {
            when(item.getSeccionId()).thenReturn(seccionId);
        }
        when(item.isVisible()).thenReturn(visible);
        return item;
    }
}
