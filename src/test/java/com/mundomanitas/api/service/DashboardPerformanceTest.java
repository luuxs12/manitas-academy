package com.mundomanitas.api.service;

import com.mundomanitas.api.model.Curso;
import com.mundomanitas.api.model.CursoModulo;
import com.mundomanitas.api.model.Inscripcion;
import com.mundomanitas.api.model.Usuario;
import com.mundomanitas.api.model.Video;
import com.mundomanitas.api.repository.CursoModuloRepository;
import com.mundomanitas.api.repository.CursoRepository;
import com.mundomanitas.api.repository.InscripcionRepository;
import com.mundomanitas.api.repository.UsuarioRepository;
import com.mundomanitas.api.repository.VideoRepository;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Consultas reales: el avance no puede crecer con la cantidad de matrículas. */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Transactional
@DisplayName("Rendimiento del avance del panel")
class DashboardPerformanceTest {

    private static final Logger log = LoggerFactory.getLogger(DashboardPerformanceTest.class);

    @Autowired private UsuarioRepository users;
    @Autowired private CursoRepository courses;
    @Autowired private CursoModuloRepository modules;
    @Autowired private VideoRepository videos;
    @Autowired private InscripcionRepository savedEnrollments;
    @Autowired private InscripcionService enrollments;
    @Autowired private EstudianteContenidoService content;
    @Autowired private EntityManager entityManager;

    @Test
    @DisplayName("el avance general resuelve varias matrículas en consultas constantes")
    void totalsUseBoundedQueries() {
        Usuario estudiante = users.save(new Usuario(
                "rendimiento-" + UUID.randomUUID() + "@ejemplo.test",
                "hash-de-prueba",
                "Prueba de rendimiento"));

        for (int index = 0; index < 2; index++) {
            Curso curso = new Curso(
                    "rendimiento-" + UUID.randomUUID(),
                    "Curso de rendimiento " + index,
                    "Inicial", 2, true, "Pruebas",
                    "Curso creado para medir consultas.", "", index);
            curso.publicar();
            curso = courses.save(curso);

            CursoModulo modulo = modules.save(new CursoModulo(curso.getId(), "Unidad 1", 0));
            videos.save(new Video(modulo.getId(), "Clase 1", "dQw4w9WgXcQ", 0));
            videos.save(new Video(modulo.getId(), "Clase 2", "dQw4w9WgXcQ", 1));
            savedEnrollments.save(new Inscripcion(estudiante, curso));
        }
        entityManager.flush();
        entityManager.clear();

        List<Inscripcion> mine = enrollments.mine(estudiante);
        assertThat(mine).hasSize(2);

        Statistics stats = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        EstudianteContenidoService.Progreso totals = content.totals(mine);
        long statements = stats.getPrepareStatementCount();

        log.info("Avance de {} matrículas resuelto en {} sentencias", mine.size(), statements);
        assertThat(totals.total()).isEqualTo(4);
        assertThat(statements).as("sentencias para %d matrículas", mine.size())
                .isLessThanOrEqualTo(2);
    }
}
