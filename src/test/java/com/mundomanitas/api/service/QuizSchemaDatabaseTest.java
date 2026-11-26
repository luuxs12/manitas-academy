package com.mundomanitas.api.service;

import com.mundomanitas.api.dto.admin.AdminCourseForm;
import com.mundomanitas.api.dto.admin.AdminExerciseForm;
import com.mundomanitas.api.dto.admin.AdminModuleForm;
import com.mundomanitas.api.model.Enrollment;
import com.mundomanitas.api.model.User;
import com.mundomanitas.api.repository.UserRepository;
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
class QuizSchemaDatabaseTest {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private DataSource dataSource;
    @Autowired private AdminCatalogService catalog;
    @Autowired private UserRepository users;
    @Autowired private EnrollmentService enrollments;

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
    void supportsTenQuestionsAndSingleAttempt() {
        Fixture fixture = fixture("curso-quiz-", "Curso quiz", "Practica 1");
        List<UUID> questionIds = new ArrayList<>();
        for (int position = 1; position <= 10; position++) {
            UUID questionId = UUID.randomUUID();
            jdbc.update("insert into quiz_questions (id, exercise_id, position, prompt, points) "
                            + "values (?, ?, ?, ?, 2)",
                    questionId, fixture.exerciseId(), position, "Pregunta " + position);
            questionIds.add(questionId);
            for (int alternative = 1; alternative <= 4; alternative++) {
                jdbc.update("insert into quiz_alternatives "
                                + "(id, question_id, position, label, is_correct) values (?, ?, ?, ?, ?)",
                        UUID.randomUUID(), questionId, alternative, "Alternativa " + alternative,
                        alternative == 1);
            }
        }

        Integer questionCount = jdbc.queryForObject(
                "select count(*) from quiz_questions where exercise_id = ?",
                Integer.class, fixture.exerciseId());
        Integer totalPoints = jdbc.queryForObject(
                "select sum(points) from quiz_questions where exercise_id = ?",
                Integer.class, fixture.exerciseId());
        assertThat(questionCount).isEqualTo(10);
        assertThat(totalPoints).isEqualTo(20);

        UUID attemptId = UUID.randomUUID();
        jdbc.update("insert into quiz_attempts (id, enrollment_id, exercise_id, score) "
                        + "values (?, ?, ?, 14)",
                attemptId, fixture.enrollmentId(), fixture.exerciseId());
        expectRejected(() -> jdbc.update(
                "insert into quiz_attempts (id, enrollment_id, exercise_id, score) "
                        + "values (?, ?, ?, 10)",
                UUID.randomUUID(), fixture.enrollmentId(), fixture.exerciseId()));

        for (UUID questionId : questionIds) {
            UUID correctId = jdbc.queryForObject(
                    "select id from quiz_alternatives where question_id = ? and is_correct",
                    UUID.class, questionId);
            jdbc.update("insert into quiz_attempt_answers "
                            + "(id, attempt_id, question_id, alternative_id, points_earned) "
                            + "values (?, ?, ?, ?, 2)",
                    UUID.randomUUID(), attemptId, questionId, correctId);
        }
        Integer answered = jdbc.queryForObject(
                "select count(*) from quiz_attempt_answers where attempt_id = ?",
                Integer.class, attemptId);
        assertThat(answered).isEqualTo(10);

        UUID firstQuestion = questionIds.get(0);
        UUID wrongId = jdbc.queryForObject(
                "select id from quiz_alternatives where question_id = ? and not is_correct limit 1",
                UUID.class, firstQuestion);
        expectRejected(() -> jdbc.update(
                "insert into quiz_attempt_answers "
                        + "(id, attempt_id, question_id, alternative_id, points_earned) "
                        + "values (?, ?, ?, ?, 0)",
                UUID.randomUUID(), attemptId, firstQuestion, wrongId));
    }

    @Test
    @DisplayName("rechaza una pregunta once, dos correctas y una alternativa de otra pregunta")
    void enforcesQuizRules() {
        Fixture fixture = fixture("curso-quiz-reglas-", "Curso reglas", "Practica");
        UUID questionId = UUID.randomUUID();
        jdbc.update("insert into quiz_questions (id, exercise_id, position, prompt, points) "
                        + "values (?, ?, 1, 'Pregunta 1', 2)", questionId, fixture.exerciseId());
        expectRejected(() -> jdbc.update(
                "insert into quiz_questions (id, exercise_id, position, prompt, points) "
                        + "values (?, ?, 11, 'Pregunta 11', 2)",
                UUID.randomUUID(), fixture.exerciseId()));

        jdbc.update("insert into quiz_alternatives "
                        + "(id, question_id, position, label, is_correct) values (?, ?, 1, 'Correcta', true)",
                UUID.randomUUID(), questionId);
        expectRejected(() -> jdbc.update(
                "insert into quiz_alternatives "
                        + "(id, question_id, position, label, is_correct) values (?, ?, 2, 'Otra correcta', true)",
                UUID.randomUUID(), questionId));

        UUID otherExercise = catalog.createExercise(new AdminExerciseForm(fixture.moduleId(),
                "Otra practica", "Otra consigna.", 1), fixture.actor());
        UUID otherQuestion = UUID.randomUUID();
        jdbc.update("insert into quiz_questions (id, exercise_id, position, prompt, points) "
                        + "values (?, ?, 1, 'Pregunta ajena', 2)", otherQuestion, otherExercise);
        UUID foreignAlternative = UUID.randomUUID();
        jdbc.update("insert into quiz_alternatives "
                        + "(id, question_id, position, label, is_correct) values (?, ?, 1, 'Ajena', true)",
                foreignAlternative, otherQuestion);

        UUID attemptId = UUID.randomUUID();
        jdbc.update("insert into quiz_attempts (id, enrollment_id, exercise_id, score) "
                        + "values (?, ?, ?, 0)", attemptId, fixture.enrollmentId(), fixture.exerciseId());
        expectRejected(() -> jdbc.update(
                "insert into quiz_attempt_answers "
                        + "(id, attempt_id, question_id, alternative_id, points_earned) "
                        + "values (?, ?, ?, ?, 2)",
                UUID.randomUUID(), attemptId, questionId, foreignAlternative));

        jdbc.update("delete from exercises where id = ?", fixture.exerciseId());
        Integer remainingQuestions = jdbc.queryForObject(
                "select count(*) from quiz_questions where exercise_id = ?",
                Integer.class, fixture.exerciseId());
        Integer remainingAttempts = jdbc.queryForObject(
                "select count(*) from quiz_attempts where exercise_id = ?",
                Integer.class, fixture.exerciseId());
        assertThat(remainingQuestions).isZero();
        assertThat(remainingAttempts).isZero();
    }

    private Fixture fixture(String slugPrefix, String courseTitle, String exerciseTitle) {
        UUID actor = tempAdmin();
        User student = users.save(new User("quiz-" + UUID.randomUUID() + "@test.local",
                "hash-de-prueba", "Alumno del cuestionario"));
        String slug = slugPrefix + UUID.randomUUID();
        UUID courseId = catalog.createCourse(new AdminCourseForm(slug, courseTitle,
                "Inicial", true, "Prueba", "Ficha del cuestionario.", "", 0), actor);
        UUID moduleId = catalog.createModule(new AdminModuleForm(courseId, "Sesion 1", 0), actor);
        UUID exerciseId = catalog.createExercise(new AdminExerciseForm(moduleId,
                exerciseTitle, "Consigna del cuestionario.", 0), actor);
        Enrollment enrollment = enrollments.enroll(student, slug);
        entityManager.flush();
        return new Fixture(actor, moduleId, exerciseId, enrollment.getId());
    }

    private void expectRejected(Runnable statement) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
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
                DataSourceUtils.releaseConnection(connection, dataSource);
            }
        }
    }

    private UUID tempAdmin() {
        User admin = new User("admin-" + UUID.randomUUID() + "@test.local", "hash",
                "Administrador de integración");
        admin.changeRole("ADMIN");
        return users.save(admin).getId();
    }

    private record Fixture(UUID actor, UUID moduleId, UUID exerciseId, UUID enrollmentId) {
    }
}
