package com.mundomanitas.api.service;

import com.mundomanitas.api.dto.admin.AdminCursoFormulario;
import com.mundomanitas.api.dto.admin.AdminEjercicioFormulario;
import com.mundomanitas.api.dto.admin.AdminModuloFormulario;
import com.mundomanitas.api.dto.admin.AdminVideoFormulario;
import com.mundomanitas.api.model.Inscripcion;
import com.mundomanitas.api.model.Usuario;
import com.mundomanitas.api.repository.ContenidoItemRepository;
import com.mundomanitas.api.repository.UsuarioRepository;
import com.mundomanitas.api.service.admin.AdminCatalogService;
import com.mundomanitas.api.service.admin.AdminCursoContenidoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@DisplayName("Estructura de contenido contra PostgreSQL")
class CursoContenidoBaseTest {

    @Autowired private AdminCatalogService catalog;
    @Autowired private AdminCursoContenidoService adminContenido;
    @Autowired private CursoContenidoService content;
    @Autowired private InscripcionService enrollments;
    @Autowired private UsuarioRepository users;
    @Autowired private ContenidoItemRepository items;

    @BeforeEach
    void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@test.local", "none",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("migra el contenido actual y lo deja navegable con avance real")
    void existingContenidoIsNavigable() {
        UUID actor = tempAdmin();
        Usuario estudiante = users.save(new Usuario("alumno-" + UUID.randomUUID() + "@test.local",
                "hash-de-prueba", "Alumno de integración"));
        String slug = "curso-contenido-" + UUID.randomUUID();
        UUID cursoId = catalog.createCurso(new AdminCursoFormulario(slug, "Contenido real",
                "Nivel inicial", true, "Pruebas", "Ficha para verificar la estructura.", "", 0), actor);
        UUID moduloId = catalog.createModulo(
                new AdminModuloFormulario(cursoId, "Unidad 1", 0), actor);
        UUID videoId = catalog.createVideo(new AdminVideoFormulario(moduloId, "Clase 1",
                0, "https://youtu.be/dQw4w9WgXcQ"), actor);
        UUID ejercicioId = catalog.createEjercicio(new AdminEjercicioFormulario(moduloId,
                "Práctica 1", "Consigna real.", 0), actor);

        UUID videoItemId = items.buscarPorOrigenTipoAndOrigenId("VIDEO", videoId).getId();
        UUID ejercicioItemId = items.buscarPorOrigenTipoAndOrigenId("EJERCICIO", ejercicioId).getId();
        assertThat(videoItemId).isNotNull();
        assertThat(ejercicioItemId).isNotNull();

        UUID seccionId = adminContenido.createSeccion(moduloId,
                new AdminCursoContenidoService.SeccionFormulario("Lecturas", 0, true), actor);
        UUID materialId = adminContenido.createItem(new AdminCursoContenidoService.ItemFormulario(
                moduloId, seccionId, "MATERIAL", "Guía de estilo", "Lectura breve",
                "https://example.org/guia", null, null, 0, true), actor);
        adminContenido.saveSilabo(cursoId, new AdminCursoContenidoService.SilaboFormulario(
                "Objetivos reales", "Requisitos reales",
                List.of(new AdminCursoContenidoService.RecursoFormulario(null, "Guía",
                        "https://example.org/guia", 0))), actor);

        Inscripcion inscripcion = enrollments.enroll(estudiante, slug);
        content.markVisto(estudiante, slug, videoItemId, true);

        CursoContenidoService.Esquema esquema = content.esquema(cursoId, inscripcion.getId(),
                estudiante, false);
        assertThat(esquema.totalItems()).isEqualTo(3);
        assertThat(esquema.doneItems()).isEqualTo(1);
        assertThat(esquema.percent()).isEqualTo(33);
        assertThat(esquema.nextPending()).isNotNull().isNotEqualTo(videoItemId);
        assertThat(esquema.modules().get(0).sections())
                .anyMatch(seccion -> seccion.id().equals(seccionId)
                        && seccion.items().stream().anyMatch(item -> item.id().equals(materialId)));
        assertThat(content.silabo(cursoId).objectives()).isEqualTo("Objetivos reales");
        assertThat(content.progreso(inscripcion.getId(), cursoId).percent()).isEqualTo(33);
    }

    private UUID tempAdmin() {
        Usuario admin = new Usuario("admin-" + UUID.randomUUID() + "@test.local", "hash",
                "Administrador de integración");
        admin.changeRol("ADMIN");
        return users.save(admin).getId();
    }
}
