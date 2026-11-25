package com.mundomanitas.api.service;

import com.mundomanitas.api.model.Certificate;
import com.mundomanitas.api.model.Course;
import com.mundomanitas.api.model.CourseModule;
import com.mundomanitas.api.model.User;
import com.mundomanitas.api.model.Video;
import com.mundomanitas.api.repository.CertificateRepository;
import com.mundomanitas.api.repository.CourseModuleRepository;
import com.mundomanitas.api.repository.CourseRepository;
import com.mundomanitas.api.repository.UserRepository;
import com.mundomanitas.api.repository.VideoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** La vista del alumno solo puede mostrar certificados activos de PostgreSQL. */
@SpringBootTest
@Transactional
@DisplayName("Certificados reales del alumno")
class StudentCertificateDatabaseTest {

    @Autowired private UserRepository users;
    @Autowired private CourseRepository courses;
    @Autowired private CourseModuleRepository modules;
    @Autowired private VideoRepository videos;
    @Autowired private CertificateRepository certificates;

    @Test
    @DisplayName("el listado trae el certificado vigente con el curso y los videos reales")
    void activeCertificateUsesRealCourseData() {
        User student = users.save(new User(
                "certificado-" + UUID.randomUUID() + "@ejemplo.test",
                "hash-de-prueba", "Prueba de certificado"));
        Course course = new Course(
                "certificado-" + UUID.randomUUID(), "Redes reales", "Inicial", 9,
                true, "Pruebas", "Curso para verificar el certificado.", "", 0);
        course.publish();
        course = courses.save(course);
        CourseModule module = modules.save(new CourseModule(course.getId(), "Unidad 1", 0));
        videos.save(new Video(module.getId(), "Clase 1", "dQw4w9WgXcQ", 0));
        videos.save(new Video(module.getId(), "Clase 2", "dQw4w9WgXcQ", 1));

        Certificate revoked = new Certificate(student.getId(), course.getId(), "MM-RED-OLD12345");
        revoked.revoke();
        certificates.save(revoked);
        Certificate active = certificates.save(
                new Certificate(student.getId(), course.getId(), "MM-RED-ABCD1234"));

        var rows = certificates.findActiveByUser(student.getId());

        assertThat(rows).hasSize(1);
        var row = rows.get(0);
        assertThat(row.getCertificateId()).isEqualTo(active.getId());
        assertThat(row.getCode()).isEqualTo("MM-RED-ABCD1234");
        assertThat(row.getCourseTitle()).isEqualTo("Redes reales");
        assertThat(row.getCourseSlug()).isEqualTo(course.getSlug());
        assertThat(row.getVideoCount()).isEqualTo(2);
        assertThat(certificates.findActiveByUserAndCourseSlug(student.getId(), course.getSlug()))
                .isPresent();
    }
}
