package com.mundomanitas.api.service;

import com.mundomanitas.api.dto.admin.AdminCursoFormulario;
import com.mundomanitas.api.dto.admin.AdminEjercicioFormulario;
import com.mundomanitas.api.dto.admin.AdminModuloFormulario;
import com.mundomanitas.api.model.Inscripcion;
import com.mundomanitas.api.model.Usuario;
import com.mundomanitas.api.repository.UsuarioRepository;
import com.mundomanitas.api.service.admin.AdminCatalogService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest
@Transactional
@DisplayName("Esquema del cuestionario contra PostgreSQL")
class CuestionarioSchemaBaseTest {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private DataSource datosOrigen;
    @Autowired private AdminCatalogService catalog;
    @Autowired private UsuarioRepository users;
    @Autowired private InscripcionService enrollments;

    @PersistenceContext private EntityManager entityManager;

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
    @DisplayName("guarda diez preguntas de dos puntos y un solo intento por matrícula")
    void supportsTenQuestionsAndSingleIntento() {
        Fixture fixture = fixture("curso-quiz-", "Curso quiz", "Practica 1");
        List<UUID> preguntaIds = new ArrayList<>();
        for (int posicion = 1; posicion <= 10; posicion++) {
            UUID preguntaId = UUID.randomUUID();
            jdbc.update("insert into quiz_questions (id, exercise_id, position, prompt, points) "
                            + "values (?, ?, ?, ?, 2)",
                    preguntaId, fixture.ejercicioId(), posicion, "Pregunta " + posicion);
            preguntaIds.add(preguntaId);
            for (int alternativa = 1; alternativa <= 4; alternativa++) {
                jdbc.update("insert into quiz_alternatives "
                                + "(id, question_id, position, label, is_correct) values (?, ?, ?, ?, ?)",
                        UUID.randomUUID(), preguntaId, alternativa, "Alternativa " + alternativa,
                        alternativa == 1);
            }
        }

        Integer preguntaContar = jdbc.queryForObject(
                "select count(*) from quiz_questions where exercise_id = ?",
                Integer.class, fixture.ejercicioId());
        Integer totalPoints = jdbc.queryForObject(
                "select sum(points) from quiz_questions where exercise_id = ?",
                Integer.class, fixture.ejercicioId());
        assertThat(preguntaContar).isEqualTo(10);
        assertThat(totalPoints).isEqualTo(20);

        UUID intentoId = UUID.randomUUID();
        jdbc.update("insert into quiz_attempts (id, enrollment_id, exercise_id, score) "
                        + "values (?, ?, ?, 14)",
                intentoId, fixture.inscripcionId(), fixture.ejercicioId());
        expectRejected(() -> jdbc.update(
                "insert into quiz_attempts (id, enrollment_id, exercise_id, score) "
                        + "values (?, ?, ?, 10)",
                UUID.randomUUID(), fixture.inscripcionId(), fixture.ejercicioId()));

        for (UUID preguntaId : preguntaIds) {
            UUID correctId = jdbc.queryForObject(
                    "select id from quiz_alternatives where question_id = ? and is_correct",
                    UUID.class, preguntaId);
            jdbc.update("insert into quiz_attempt_answers "
                            + "(id, attempt_id, question_id, alternative_id, points_earned) "
                            + "values (?, ?, ?, ?, 2)",
                    UUID.randomUUID(), intentoId, preguntaId, correctId);
        }
        Integer answered = jdbc.queryForObject(
                "select count(*) from quiz_attempt_answers where attempt_id = ?",
                Integer.class, intentoId);
        assertThat(answered).isEqualTo(10);

        UUID firstPregunta = preguntaIds.get(0);
        UUID incorrectaId = jdbc.queryForObject(
                "select id from quiz_alternatives where question_id = ? and not is_correct limit 1",
                UUID.class, firstPregunta);
        expectRejected(() -> jdbc.update(
                "insert into quiz_attempt_answers "
                        + "(id, attempt_id, question_id, alternative_id, points_earned) "
                        + "values (?, ?, ?, ?, 0)",
                UUID.randomUUID(), intentoId, firstPregunta, incorrectaId));
    }

    @Test
    @DisplayName("rechaza una pregunta once, dos correctas y una alternativa de otra pregunta")
    void enforcesCuestionarioRules() {
        Fixture fixture = fixture("curso-quiz-reglas-", "Curso reglas", "Practica");
        UUID preguntaId = UUID.randomUUID();
        jdbc.update("insert into quiz_questions (id, exercise_id, position, prompt, points) "
                        + "values (?, ?, 1, 'Pregunta 1', 2)", preguntaId, fixture.ejercicioId());
        expectRejected(() -> jdbc.update(
                "insert into quiz_questions (id, exercise_id, position, prompt, points) "
                        + "values (?, ?, 11, 'Pregunta 11', 2)",
                UUID.randomUUID(), fixture.ejercicioId()));

        jdbc.update("insert into quiz_alternatives "
                        + "(id, question_id, position, label, is_correct) values (?, ?, 1, 'Correcta', true)",
                UUID.randomUUID(), preguntaId);
        expectRejected(() -> jdbc.update(
                "insert into quiz_alternatives "
                        + "(id, question_id, position, label, is_correct) values (?, ?, 2, 'Otra correcta', true)",
                UUID.randomUUID(), preguntaId));

        UUID otherEjercicio = catalog.createEjercicio(new AdminEjercicioFormulario(fixture.moduloId(),
                "Otra practica", "Otra consigna.", 1), fixture.actor());
        UUID otherPregunta = UUID.randomUUID();
        jdbc.update("insert into quiz_questions (id, exercise_id, position, prompt, points) "
                        + "values (?, ?, 1, 'Pregunta ajena', 2)", otherPregunta, otherEjercicio);
        UUID foreignAlternativa = UUID.randomUUID();
        jdbc.update("insert into quiz_alternatives "
                        + "(id, question_id, position, label, is_correct) values (?, ?, 1, 'Ajena', true)",
                foreignAlternativa, otherPregunta);

        UUID intentoId = UUID.randomUUID();
        jdbc.update("insert into quiz_attempts (id, enrollment_id, exercise_id, score) "
                        + "values (?, ?, ?, 0)", intentoId, fixture.inscripcionId(), fixture.ejercicioId());
        expectRejected(() -> jdbc.update(
                "insert into quiz_attempt_answers "
                        + "(id, attempt_id, question_id, alternative_id, points_earned) "
                        + "values (?, ?, ?, ?, 2)",
                UUID.randomUUID(), intentoId, preguntaId, foreignAlternativa));

        jdbc.update("delete from exercises where id = ?", fixture.ejercicioId());
        Integer remainingQuestions = jdbc.queryForObject(
                "select count(*) from quiz_questions where exercise_id = ?",
                Integer.class, fixture.ejercicioId());
        Integer remainingAttempts = jdbc.queryForObject(
                "select count(*) from quiz_attempts where exercise_id = ?",
                Integer.class, fixture.ejercicioId());
        assertThat(remainingQuestions).isZero();
        assertThat(remainingAttempts).isZero();
    }

    private Fixture fixture(String slugPrefix, String cursoTitulo, String ejercicioTitulo) {
        UUID actor = tempAdmin();
        Usuario estudiante = users.save(new Usuario("quiz-" + UUID.randomUUID() + "@test.local",
                "hash-de-prueba", "Alumno del cuestionario"));
        String slug = slugPrefix + UUID.randomUUID();
        UUID cursoId = catalog.createCurso(new AdminCursoFormulario(slug, cursoTitulo,
                "Inicial", true, "Prueba", "Ficha del cuestionario.", "", 0), actor);
        UUID moduloId = catalog.createModulo(new AdminModuloFormulario(cursoId, "Sesion 1", 0), actor);
        UUID ejercicioId = catalog.createEjercicio(new AdminEjercicioFormulario(moduloId,
                ejercicioTitulo, "Consigna del cuestionario.", 0), actor);
        Inscripcion inscripcion = enrollments.enroll(estudiante, slug);
        entityManager.flush();
        return new Fixture(actor, moduloId, ejercicioId, inscripcion.getId());
    }

    private void expectRejected(Runnable statement) {
        Connection connection = DataSourceUtils.getConnection(datosOrigen);
        Savepoint savepoint;
        try {
            savepoint = connection.setSavepoint();
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
        try {
            statement.run();
            fail("Se esperaba que la base rechazara la sentencia");
        } catch (DataAccessException expected) {
            // La restricción hizo su trabajo; se deshace solo ese punto.
        } finally {
            try {
                connection.rollback(savepoint);
            } catch (SQLException exception) {
                throw new IllegalStateException(exception);
            } finally {
                DataSourceUtils.releaseConnection(connection, datosOrigen);
            }
        }
    }

    private UUID tempAdmin() {
        Usuario admin = new Usuario("admin-" + UUID.randomUUID() + "@test.local", "hash",
                "Administrador de integración");
        admin.changeRol("ADMIN");
        return users.save(admin).getId();
    }

    private record Fixture(UUID actor, UUID moduloId, UUID ejercicioId, UUID inscripcionId) {
    }
}
