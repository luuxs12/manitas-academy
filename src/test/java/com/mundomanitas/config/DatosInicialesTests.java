package com.mundomanitas.config;

import com.mundomanitas.repository.EjercicioRepository;
import com.mundomanitas.repository.InscripcionRepository;
import com.mundomanitas.repository.ModuloRepository;
import com.mundomanitas.repository.ModuloSistemaRepository;
import com.mundomanitas.repository.PostulacionDocenteRepository;
import com.mundomanitas.repository.ProgresoRepository;
import com.mundomanitas.repository.RolRepository;
import com.mundomanitas.repository.UsuarioRepository;
import com.mundomanitas.repository.VideoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica que las semillas creen los datos mínimos sobre una base vacía
 * (H2 arranca sin tablas ni registros) y que ejecutarlas de nuevo no
 * duplique información.
 *
 * <p>Se fuerza un contexto limpio antes de la clase porque otras pruebas
 * pueden dejar usuarios o postulaciones en la misma base H2 compartida,
 * haciendo que los conteos exactos dependan del orden de ejecución.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class DatosInicialesTests {

    @Autowired
    @Qualifier("crearDatosIniciales")
    private CommandLineRunner semillas;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private ModuloSistemaRepository moduloSistemaRepository;

    @Autowired
    private ModuloRepository moduloRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private EjercicioRepository ejercicioRepository;

    @Autowired
    private ProgresoRepository progresoRepository;

    @Autowired
    private InscripcionRepository inscripcionRepository;

    @Autowired
    private PostulacionDocenteRepository postulacionRepository;

    @Test
    void laBaseVaciaQuedaSembradaConUsuariosRolesYContenido() {
        assertEquals(8, usuarioRepository.count(), "Deben existir los ocho usuarios demo");
        assertEquals(5, rolRepository.count(), "Deben existir los cinco roles del sistema");
        assertEquals(8, moduloSistemaRepository.count(), "Deben existir los ocho módulos de navegación");

        assertTrue(moduloRepository.count() >= 4);
        assertTrue(videoRepository.count() >= 3);
        assertTrue(ejercicioRepository.count() >= 2);
        assertTrue(progresoRepository.count() >= 4);
        assertTrue(inscripcionRepository.count() >= 4);

        assertTrue(usuarioRepository.findByEmailIgnoreCase("alumno@mundomanitas.com").isPresent());
        assertTrue(usuarioRepository.findByEmailIgnoreCase("maria@mundomanitas.com").isPresent());
        assertTrue(usuarioRepository.findByEmailIgnoreCase("docente@mundomanitas.com").isPresent());
        assertTrue(usuarioRepository.findByEmailIgnoreCase("admin@mundomanitas.com").isPresent());
        assertTrue(usuarioRepository.findByEmailIgnoreCase("sistemas@mundomanitas.com").isPresent());
    }

    @Test
    void losCursosDemoTienenDocenteAsignado() {
        var docente = usuarioRepository.findByEmailIgnoreCase("docente@mundomanitas.com").orElseThrow();

        assertTrue(moduloRepository.count() > 0);
        assertTrue(
            moduloRepository.findAllByOrderByOrdenAsc().stream()
                .allMatch(modulo -> docente.getIdUsuario().equals(modulo.getIdDocente())),
            "Todos los cursos demo deben pertenecer al docente de ejemplo"
        );
    }

    @Test
    void ejecutarLasSemillasDeNuevoNoDuplicaRegistros() throws Exception {
        long usuarios = usuarioRepository.count();
        long roles = rolRepository.count();
        long modulosSistema = moduloSistemaRepository.count();
        long modulos = moduloRepository.count();
        long videos = videoRepository.count();
        long ejercicios = ejercicioRepository.count();
        long progresos = progresoRepository.count();
        long inscripciones = inscripcionRepository.count();
        long postulaciones = postulacionRepository.count();

        semillas.run();

        assertEquals(usuarios, usuarioRepository.count());
        assertEquals(roles, rolRepository.count());
        assertEquals(modulosSistema, moduloSistemaRepository.count());
        assertEquals(modulos, moduloRepository.count());
        assertEquals(videos, videoRepository.count());
        assertEquals(ejercicios, ejercicioRepository.count());
        assertEquals(progresos, progresoRepository.count());
        assertEquals(inscripciones, inscripcionRepository.count());
        assertEquals(postulaciones, postulacionRepository.count());
    }

    @Test
    void losRolesTienenAsignadosSusModulosDeNavegacion() {
        var estudiante = rolRepository.findByCodigoIgnoreCase("ESTUDIANTE").orElseThrow();
        var docente = rolRepository.findByCodigoIgnoreCase("DOCENTE").orElseThrow();
        var administrador = rolRepository.findByCodigoIgnoreCase("ADMINISTRADOR").orElseThrow();

        assertNotNull(estudiante.getModulos());
        assertTrue(estudiante.getModulos().size() >= 3);
        assertTrue(docente.getModulos().stream().anyMatch(m -> "ADMIN_CONTENIDO".equals(m.getCodigo())));
        assertTrue(docente.getModulos().stream().anyMatch(m -> "DOCENTE_CURSOS".equals(m.getCodigo())));
        assertTrue(administrador.getModulos().stream().anyMatch(m -> "ADMIN_USUARIOS".equals(m.getCodigo())));
        assertTrue(administrador.getModulos().stream().anyMatch(m -> "ADMIN_PANEL".equals(m.getCodigo())));
        assertTrue(administrador.getModulos().stream().anyMatch(m -> "ADMIN_POSTULACIONES".equals(m.getCodigo())));
    }

    @Test
    void laPostulacionDocenteDemoQuedaPendienteDeRevision() {
        assertEquals(1, postulacionRepository.count());
        assertTrue(postulacionRepository.findAll().stream()
            .allMatch(postulacion -> postulacion.getEstado().estaPendiente()));
    }
}
