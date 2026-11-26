package com.mundomanitas.api.service.admin;

import com.mundomanitas.api.dto.admin.AdminCursoFormulario;
import com.mundomanitas.api.dto.admin.AdminEjercicioFormulario;
import com.mundomanitas.api.dto.admin.AdminModuloFormulario;
import com.mundomanitas.api.dto.admin.AdminVideoFormulario;
import com.mundomanitas.api.model.Inscripcion;
import com.mundomanitas.api.model.Usuario;
import com.mundomanitas.api.repository.UsuarioRepository;
import com.mundomanitas.api.service.CursoCatalog;
import com.mundomanitas.api.service.InscripcionService;
import com.mundomanitas.api.service.EstudianteContenidoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@DisplayName("Catálogo administrativo contra PostgreSQL")
class AdminCatalogBaseTest {

    @Autowired
    private AdminCatalogService catalog;

    @Autowired
    private UsuarioRepository users;

    @Autowired
    private CursoCatalog estudianteCatalog;

    @Autowired
    private InscripcionService enrollments;

    @Autowired
    private EstudianteContenidoService content;

    @Test
    @DisplayName("crea un curso visible y lo deja inscribible con su contenido real")
    void createsCursoAndContenido() {
        UUID actor = tempAdmin();
        Usuario estudiante = users.save(new Usuario("alumno-" + UUID.randomUUID() + "@test.local",
                "hash-de-prueba", "Alumno de integración"));
        String slug = "curso-integracion-" + UUID.randomUUID();
        UUID cursoId = catalog.createCurso(new AdminCursoFormulario(
                slug, "Curso de integración", "Nivel inicial", true, "Pruebas",
                "Ficha real para verificar el alta sin portada.", "", 0), actor);

        assertThat(catalog.cursoFormulario(cursoId).imagen()).isNull();
        assertThat(catalog.cursoEstado(cursoId)).isEqualTo("PUBLICADO");
        assertThat(estudianteCatalog.all()).anyMatch(curso -> curso.getSlug().equals(slug));

        UUID moduloId = catalog.createModulo(
                new AdminModuloFormulario(cursoId, "Unidad de integración", 0), actor);
        UUID videoId = catalog.createVideo(new AdminVideoFormulario(moduloId, "Clase de integración",
                0, "https://youtu.be/dQw4w9WgXcQ"), actor);
        UUID ejercicioId = catalog.createEjercicio(new AdminEjercicioFormulario(moduloId,
                "Práctica de integración", "Consigna de integración.", 0), actor);

        assertThat(moduloId).isNotNull();
        assertThat(videoId).isNotNull();
        assertThat(ejercicioId).isNotNull();
        assertThat(catalog.videos(null, null, PageRequest.of(0, 10)).getContent())
                .anyMatch(row -> row.titulo().equals("Clase de integración"));
        assertThat(catalog.modules(cursoId, null, PageRequest.of(0, 10)).getContent())
                .anyMatch(row -> row.titulo().equals("Unidad de integración"));

        Inscripcion inscripcion = enrollments.enroll(estudiante, slug);
        assertThat(enrollments.buscarMine(estudiante, slug).getId()).isEqualTo(inscripcion.getId());
        assertThat(content.esquema(cursoId).modules())
                .anyMatch(modulo -> modulo.titulo().equals("Unidad de integración")
                        && modulo.videos().stream().anyMatch(v -> v.id().equals(videoId)));
    }

    private UUID tempAdmin() {
        String suffix = UUID.randomUUID().toString();
        Usuario admin = new Usuario("admin-" + suffix + "@test.local", "hash-de-prueba",
                "Administrador de integración");
        admin.changeRol("ADMIN");
        return users.save(admin).getId();
    }
}
