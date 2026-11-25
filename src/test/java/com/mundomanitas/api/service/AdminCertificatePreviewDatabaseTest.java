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

/** La vista previa del admin lee los certificados reales, vigentes o revocados. */
@SpringBootTest
@Transactional
@DisplayName("Vista previa real de certificados")
class AdminCertificatePreviewDatabaseTest {

    @Autowired private UserRepository users;
    @Autowired private CourseRepository courses;
    @Autowired private CourseModuleRepository modules;
    @Autowired private VideoRepository videos;
    @Autowired private CertificateRepository certificates;

    @Test
    @DisplayName("el certificado vigente muestra la persona, el curso y los videos de la base")
    void activeCertificateUsesRealData() {
        User student = users.save(new User(
                "vista-previa-" + UUID.randomUUID() + "@ejemplo.test",
                "hash-de-prueba", "Ana Quispe"));
        Course course = new Course(
                "vista-previa-" + UUID.randomUUID(), "Redes reales", "Inicial", 9,
                true, "Pruebas", "Curso para verificar la vista previa.", "", 0);
        course.publish();
        course = courses.save(course);
        CourseModule module = modules.save(new CourseModule(course.getId(), "Unidad 1", 0));
        videos.save(new Video(module.getId(), "Clase 1", "dQw4w9WgXcQ", 0));
        videos.save(new Video(module.getId(), "Clase 2", "dQw4w9WgXcQ", 1));

        Certificate active = certificates.save(
                new Certificate(student.getId(), course.getId(), "MM-RED-ABCD1234"));

        var row = certificates.findPreviewById(active.getId()).orElseThrow();

        assertThat(row.getCertificateId()).isEqualTo(active.getId());
        assertThat(row.getCode()).isEqualTo("MM-RED-ABCD1234");
        assertThat(row.getPerson()).isEqualTo("Ana Quispe");
        assertThat(row.getCourseTitle()).isEqualTo("Redes reales");
        assertThat(row.getVideoCount()).isEqualTo(2);
        assertThat(row.getIssuedAt()).isNotNull();
        assertThat(row.isActive()).isTrue();
    }

    @Test
    @DisplayName("un certificado revocado también se puede visualizar")
    void revokedCertificateCanBePreviewed() {
        User student = users.save(new User(
                "revocada-" + UUID.randomUUID() + "@ejemplo.test",
                "hash-de-prueba", "Ana Quispe"));
        Course course = courses.save(new Course(
                "revocada-" + UUID.randomUUID(), "Redes reales", "Inicial", 9,
                true, "Pruebas", "Curso para verificar la revocación.", "", 0));
        Certificate revoked = new Certificate(
                student.getId(), course.getId(), "MM-RED-OLD12345");
        revoked.revoke();
        certificates.save(revoked);

        var row = certificates.findPreviewById(revoked.getId()).orElseThrow();

        assertThat(row.getCode()).isEqualTo("MM-RED-OLD12345");
        assertThat(row.getPerson()).isEqualTo("Ana Quispe");
        assertThat(row.getCourseTitle()).isEqualTo("Redes reales");
        assertThat(row.isActive()).isFalse();
    }
}
