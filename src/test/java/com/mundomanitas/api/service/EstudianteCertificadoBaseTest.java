package com.mundomanitas.api.service;

import com.mundomanitas.api.model.Certificado;
import com.mundomanitas.api.model.Curso;
import com.mundomanitas.api.model.CursoModulo;
import com.mundomanitas.api.model.Usuario;
import com.mundomanitas.api.model.Video;
import com.mundomanitas.api.repository.CertificadoRepository;
import com.mundomanitas.api.repository.CursoModuloRepository;
import com.mundomanitas.api.repository.CursoRepository;
import com.mundomanitas.api.repository.UsuarioRepository;
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
class EstudianteCertificadoBaseTest {

    @Autowired private UsuarioRepository users;
    @Autowired private CursoRepository courses;
    @Autowired private CursoModuloRepository modules;
    @Autowired private VideoRepository videos;
    @Autowired private CertificadoRepository certificates;

    @Test
    @DisplayName("el listado trae el certificado vigente con el curso y los videos reales")
    void activoCertificadoUsesRealCursoDatos() {
        Usuario estudiante = users.save(new Usuario(
                "certificado-" + UUID.randomUUID() + "@ejemplo.test",
                "hash-de-prueba", "Prueba de certificado"));
        Curso curso = new Curso(
                "certificado-" + UUID.randomUUID(), "Redes reales", "Inicial", 9,
                true, "Pruebas", "Curso para verificar el certificado.", "", 0);
        curso.publicar();
        curso = courses.save(curso);
        CursoModulo modulo = modules.save(new CursoModulo(curso.getId(), "Unidad 1", 0));
        videos.save(new Video(modulo.getId(), "Clase 1", "dQw4w9WgXcQ", 0));
        videos.save(new Video(modulo.getId(), "Clase 2", "dQw4w9WgXcQ", 1));

        Certificado revocado = new Certificado(estudiante.getId(), curso.getId(), "MM-RED-OLD12345");
        revocado.revoke();
        certificates.save(revocado);
        Certificado activo = certificates.save(
                new Certificado(estudiante.getId(), curso.getId(), "MM-RED-ABCD1234"));

        var rows = certificates.buscarActivoPorUsuario(estudiante.getId());

        assertThat(rows).hasSize(1);
        var row = rows.get(0);
        assertThat(row.getCertificadoId()).isEqualTo(activo.getId());
        assertThat(row.getCode()).isEqualTo("MM-RED-ABCD1234");
        assertThat(row.getCursoTitulo()).isEqualTo("Redes reales");
        assertThat(row.getCursoSlug()).isEqualTo(curso.getSlug());
        assertThat(row.getVideoContar()).isEqualTo(2);
        assertThat(certificates.buscarActivoPorUsuarioAndCursoSlug(estudiante.getId(), curso.getSlug()))
                .isPresent();
    }
}
