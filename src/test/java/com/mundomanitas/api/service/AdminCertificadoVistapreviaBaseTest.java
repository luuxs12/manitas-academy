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

/** La vista previa del admin lee los certificados reales, vigentes o revocados. */
@SpringBootTest
@Transactional
@DisplayName("Vista previa real de certificados")
class AdminCertificadoVistapreviaBaseTest {

    @Autowired private UsuarioRepository users;
    @Autowired private CursoRepository courses;
    @Autowired private CursoModuloRepository modules;
    @Autowired private VideoRepository videos;
    @Autowired private CertificadoRepository certificates;

    @Test
    @DisplayName("el certificado vigente muestra la persona, el curso y los videos de la base")
    void activoCertificadoUsesRealDatos() {
        Usuario estudiante = users.save(new Usuario(
                "vista-previa-" + UUID.randomUUID() + "@ejemplo.test",
                "hash-de-prueba", "Ana Quispe"));
        Curso curso = new Curso(
                "vista-previa-" + UUID.randomUUID(), "Redes reales", "Inicial", 9,
                true, "Pruebas", "Curso para verificar la vista previa.", "", 0);
        curso.publicar();
        curso = courses.save(curso);
        CursoModulo modulo = modules.save(new CursoModulo(curso.getId(), "Unidad 1", 0));
        videos.save(new Video(modulo.getId(), "Clase 1", "dQw4w9WgXcQ", 0));
        videos.save(new Video(modulo.getId(), "Clase 2", "dQw4w9WgXcQ", 1));

        Certificado activo = certificates.save(
                new Certificado(estudiante.getId(), curso.getId(), "MM-RED-ABCD1234"));

        var row = certificates.buscarVistapreviaPorId(activo.getId()).orElseThrow();

        assertThat(row.getCertificadoId()).isEqualTo(activo.getId());
        assertThat(row.getCode()).isEqualTo("MM-RED-ABCD1234");
        assertThat(row.getPerson()).isEqualTo("Ana Quispe");
        assertThat(row.getCursoTitulo()).isEqualTo("Redes reales");
        assertThat(row.getVideoContar()).isEqualTo(2);
        assertThat(row.getFechaEmision()).isNotNull();
        assertThat(row.isActivo()).isTrue();
    }

    @Test
    @DisplayName("un certificado revocado también se puede visualizar")
    void revocadoCertificadoCanBePreviewed() {
        Usuario estudiante = users.save(new Usuario(
                "revocada-" + UUID.randomUUID() + "@ejemplo.test",
                "hash-de-prueba", "Ana Quispe"));
        Curso curso = courses.save(new Curso(
                "revocada-" + UUID.randomUUID(), "Redes reales", "Inicial", 9,
                true, "Pruebas", "Curso para verificar la revocación.", "", 0));
        Certificado revocado = new Certificado(
                estudiante.getId(), curso.getId(), "MM-RED-OLD12345");
        revocado.revoke();
        certificates.save(revocado);

        var row = certificates.buscarVistapreviaPorId(revocado.getId()).orElseThrow();

        assertThat(row.getCode()).isEqualTo("MM-RED-OLD12345");
        assertThat(row.getPerson()).isEqualTo("Ana Quispe");
        assertThat(row.getCursoTitulo()).isEqualTo("Redes reales");
        assertThat(row.isActivo()).isFalse();
    }
}
