package com.mundomanitas.api.web;

import com.mundomanitas.api.controller.EstudianteController;
import com.mundomanitas.api.model.Curso;
import com.mundomanitas.api.model.Inscripcion;
import com.mundomanitas.api.model.Usuario;
import com.mundomanitas.api.security.AutenticadoUsuario;
import com.mundomanitas.api.service.CuentaService;
import com.mundomanitas.api.service.CursoCatalog;
import com.mundomanitas.api.service.CursoContenidoService;
import com.mundomanitas.api.service.InscripcionService;
import com.mundomanitas.api.service.LeccionProgresoService;
import com.mundomanitas.api.service.EstudianteCertificadoService;
import com.mundomanitas.api.service.EstudianteContenidoService;
import com.mundomanitas.api.service.VideoReproduccionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@DisplayName("Contenido estructurado en la vista del estudiante")
class EstudianteContenidoFlujoTest {

    private final String slug = "python";
    private UUID actor;
    private UUID cursoId;
    private UUID inscripcionId;
    private Usuario estudiante;
    private Curso curso;
    private Inscripcion inscripcion;
    private CursoContenidoService content;
    private VideoReproduccionService reproduccion;
    private CursoCatalog catalog;
    private InscripcionService enrollments;
    private EstudianteContenidoService oldContenido;
    private EstudianteCertificadoService certificates;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        actor = UUID.randomUUID();
        cursoId = UUID.randomUUID();
        inscripcionId = UUID.randomUUID();

        estudiante = mock(Usuario.class);
        when(estudiante.getId()).thenReturn(actor);
        when(estudiante.getRol()).thenReturn("STUDENT");
        AutenticadoUsuario principal = new AutenticadoUsuario(estudiante);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, "sin-clave", principal.getAuthorities()));

        CuentaService accounts = mock(CuentaService.class);
        catalog = mock(CursoCatalog.class);
        enrollments = mock(InscripcionService.class);
        LeccionProgresoService lessons = mock(LeccionProgresoService.class);
        oldContenido = mock(EstudianteContenidoService.class);
        content = mock(CursoContenidoService.class);
        reproduccion = mock(VideoReproduccionService.class);
        certificates = mock(EstudianteCertificadoService.class);

        curso = mock(Curso.class);
        when(curso.getId()).thenReturn(cursoId);
        when(curso.getSlug()).thenReturn(slug);
        when(curso.getTitulo()).thenReturn("Python");

        inscripcion = mock(Inscripcion.class);
        when(inscripcion.getId()).thenReturn(inscripcionId);
        when(inscripcion.getCurso()).thenReturn(curso);

        when(accounts.require(actor)).thenReturn(estudiante);
        when(enrollments.buscarMine(estudiante, slug)).thenReturn(inscripcion);
        when(lessons.finalizadoVideos(inscripcionId)).thenReturn(Set.of());
        when(content.esquema(cursoId, inscripcionId, estudiante, false))
                .thenReturn(new CursoContenidoService.Esquema(List.of(), 0, 0, 0, null));

        mvc = MockMvcBuilders.standaloneSetup(new EstudianteController(
                        accounts, catalog, enrollments, lessons, reproduccion,
                        oldContenido, content, certificates))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("el panel entrega a cada tarjeta el avance real de su matrícula")
    void dashboardUsesEachInscripcionProgreso() throws Exception {
        UUID dashboardInscripcionId = UUID.randomUUID();
        Curso dashboardCurso = mock(Curso.class);
        when(dashboardCurso.getId()).thenReturn(cursoId);
        when(dashboardCurso.getSlug()).thenReturn(slug);
        when(dashboardCurso.getTitulo()).thenReturn("Python");
        Inscripcion dashboardInscripcion = mock(Inscripcion.class);
        when(dashboardInscripcion.getId()).thenReturn(dashboardInscripcionId);
        when(dashboardInscripcion.getCurso()).thenReturn(dashboardCurso);

        when(enrollments.mine(estudiante)).thenReturn(List.of(dashboardInscripcion));
        when(oldContenido.progresoFor(List.of(dashboardInscripcion)))
                .thenReturn(Map.of(dashboardInscripcionId,
                        new EstudianteContenidoService.Progreso(3, 10, 30)));
        when(catalog.all()).thenReturn(List.of());

        mvc.perform(get("/panel"))
                .andExpect(view().name("student/panel"))
                .andExpect(model().attribute("doneCounts", Map.of(dashboardInscripcionId, 3)))
                .andExpect(model().attribute("videoCounts", Map.of(dashboardInscripcionId, 10)))
                .andExpect(model().attribute("videoProgreso", Map.of(dashboardInscripcionId, 30)))
                .andExpect(model().attribute("videoTotals",
                        new LeccionProgresoService.LeccionTotals(3, 10, 30)));

        verify(oldContenido).progresoFor(List.of(dashboardInscripcion));
    }

    @Test
    @DisplayName("muestra los módulos, secciones e ítems del servicio estructurado")
    void usesStructuredEsquema() throws Exception {
        UUID itemId = UUID.randomUUID();
        CursoContenidoService.ItemVista item = new CursoContenidoService.ItemVista(itemId, "MATERIAL",
                "Guía", "Lectura real", "https://example.org/guia", null, null,
                true, false, false, null);
        CursoContenidoService.Esquema esquema = new CursoContenidoService.Esquema(
                List.of(new CursoContenidoService.ModuloVista(UUID.randomUUID(), "Unidad 1", true,
                        List.of(), List.of(item), 0)),
                1, 0, 0, itemId);
        when(content.esquema(cursoId, inscripcionId, estudiante, false)).thenReturn(esquema);

        mvc.perform(get("/panel/cursos/{slug}", slug))
                .andExpect(view().name("student/curso"))
                .andExpect(model().attribute("modules", esquema.modules()))
                .andExpect(model().attribute("itemContar", 1))
                .andExpect(model().attribute("doneContar", 0))
                .andExpect(model().attribute("itemPercent", 0));

        verify(content).esquema(cursoId, inscripcionId, estudiante, false);
    }

    @Test
    @DisplayName("abre la clase desde el id real del video guardado en el ítem")
    void opensVideoFromOrigenId() throws Exception {
        UUID videoId = UUID.randomUUID();
        VideoReproduccionService.Reproduccion player = new VideoReproduccionService.Reproduccion(
                "Clase 1", "https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ");
        when(reproduccion.play(videoId, inscripcion)).thenReturn(player);

        mvc.perform(get("/panel/cursos/{slug}", slug).param("video", videoId.toString()))
                .andExpect(model().attribute("player", player))
                .andExpect(model().attribute("selectedId", videoId));

        verify(reproduccion).play(videoId, inscripcion);
    }

    @Test
    @DisplayName("abre el PDF del temario al lado del temario con sus datos reales")
    void opensMaterialBesideSilabo() throws Exception {
        UUID itemId = UUID.randomUUID();
        CursoContenidoService.MaterialVista material = new CursoContenidoService.MaterialVista(
                itemId, "Guía de la clase 01", "guia-de-clase-01.pdf", "application/pdf");
        when(content.vistaMaterial(estudiante, slug, itemId)).thenReturn(material);

        mvc.perform(get("/panel/cursos/{slug}", slug).param("material", itemId.toString()))
                .andExpect(view().name("student/curso"))
                .andExpect(model().attribute("material", material))
                .andExpect(model().attribute("selectedItemId", itemId));

        verify(content).vistaMaterial(estudiante, slug, itemId);
    }

    @Test
    @DisplayName("sirve el PDF en línea para el visor, con la matrícula validada")
    void servesPdfInline() throws Exception {
        UUID itemId = UUID.randomUUID();
        byte[] bytes = "%PDF-1.7 contenido".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        when(content.leidoPdf(estudiante, slug, itemId)).thenReturn(
                new CursoContenidoService.MaterialDescarga("guia de clase 01.pdf", "application/pdf", bytes));

        mvc.perform(get("/panel/cursos/{slug}/materiales/{itemId}/ver", slug, itemId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition",
                        "inline; filename=\"guia de clase 01.pdf\""))
                .andExpect(content().bytes(bytes));

        verify(content).leidoPdf(estudiante, slug, itemId);
    }

    @Test
    @DisplayName("un documento que no es PDF no abre el visor")
    void refusesNonPdfViewer() throws Exception {
        UUID itemId = UUID.randomUUID();
        when(content.leidoPdf(estudiante, slug, itemId)).thenThrow(
                new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));

        mvc.perform(get("/panel/cursos/{slug}/materiales/{itemId}/ver", slug, itemId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("guardar y deshacer el avance de materiales pasa por el servicio con la matrícula")
    void marksItemsVisto() throws Exception {
        UUID itemId = UUID.randomUUID();

        mvc.perform(post("/panel/cursos/{slug}/items/{itemId}/visto", slug, itemId)
                        .param("visto", "true"))
                .andExpect(redirectedUrl("/panel/cursos/" + slug))
                .andExpect(flash().attribute("panelOk", "Avance guardado en «Python»."));

        mvc.perform(post("/panel/cursos/{slug}/items/{itemId}/visto", slug, itemId)
                        .param("visto", "false"))
                .andExpect(redirectedUrl("/panel/cursos/" + slug));

        verify(content).markVisto(estudiante, slug, itemId, true);
        verify(content).markVisto(estudiante, slug, itemId, false);
    }

    @Test
    @DisplayName("el listado entrega los certificados emitidos reales")
    void certificatesListUsesEmitidoRecords() throws Exception {
        EstudianteCertificadoService.Emitido emitido = new EstudianteCertificadoService.Emitido(
                UUID.randomUUID(), "MM-RED-ABCD1234",
                OffsetDateTime.parse("2026-11-20T10:15:00Z"),
                "Redes", "redes", 3);
        when(certificates.disponible(estudiante)).thenReturn(List.of(emitido));

        mvc.perform(get("/panel/certificados"))
                .andExpect(view().name("student/certificados"))
                .andExpect(model().attribute("certificates", List.of(emitido)))
                .andExpect(model().attribute("finalizado", 0));

        verify(certificates).disponible(estudiante);
    }

    @Test
    @DisplayName("la ficha abre el certificado vigente de ese curso")
    void certificadoDetailUsesEmitidoRecord() throws Exception {
        EstudianteCertificadoService.Emitido emitido = new EstudianteCertificadoService.Emitido(
                UUID.randomUUID(), "MM-PYT-1234ABCD",
                OffsetDateTime.parse("2026-11-21T10:15:00Z"),
                "Python", slug, 5);
        when(certificates.buscarActivo(estudiante, slug)).thenReturn(Optional.of(emitido));

        mvc.perform(get("/panel/certificados/{slug}", slug))
                .andExpect(view().name("student/certificado"))
                .andExpect(model().attribute("certificado", emitido))
                .andExpect(model().attribute("cuenta", estudiante));

        verify(certificates).buscarActivo(estudiante, slug);
    }

    @Test
    @DisplayName("un certificado revocado no abre su ficha")
    void revocadoCertificadoIsNotFound() throws Exception {
        when(certificates.buscarActivo(estudiante, slug)).thenReturn(Optional.empty());

        mvc.perform(get("/panel/certificados/{slug}", slug))
                .andExpect(status().isNotFound());
    }
}
