package com.mundomanitas.api.service.admin;

import com.mundomanitas.api.dto.admin.ResultadoExportacion;
import com.mundomanitas.api.model.ContenidoItem;
import com.mundomanitas.api.model.CursoModulo;
import com.mundomanitas.api.model.CursoSeccion;
import com.mundomanitas.api.repository.ContenidoItemRepository;
import com.mundomanitas.api.repository.CursoModuloRepository;
import com.mundomanitas.api.repository.CursoRepository;
import com.mundomanitas.api.repository.CursoSeccionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Arma el PDF de una sesión con los títulos y estados que ya están en PostgreSQL. */
@Service
@PreAuthorize("hasRole('ADMIN')")
public class AdminSesionPdfServiceImpl implements AdminSesionPdfService {

    private static final Logger log = LoggerFactory.getLogger(AdminSesionPdfServiceImpl.class);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final CursoRepository courses;
    private final CursoModuloRepository modules;
    private final CursoSeccionRepository sections;
    private final ContenidoItemRepository items;
    private final AdminAuditoriaService auditoria;
    private final SimplePdfEscritor pdf;

    public AdminSesionPdfServiceImpl(CursoRepository courses,
                                      CursoModuloRepository modules,
                                      CursoSeccionRepository sections,
                                      ContenidoItemRepository items,
                                      AdminAuditoriaService auditoria,
                                      SimplePdfEscritor pdf) {
        this.courses = courses;
        this.modules = modules;
        this.sections = sections;
        this.items = items;
        this.auditoria = auditoria;
        this.pdf = pdf;
    }

    @Override
    @Transactional(readOnly = true)
    public ResultadoExportacion moduloPdf(UUID moduloId, UUID actor) {
        CursoModulo modulo = modules.findById(moduloId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "El módulo solicitado no existe."));
        String cursoTitulo = courses.findById(modulo.getCursoId())
                .map(curso -> curso.getTitulo())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "El curso de la sesión no existe."));

        List<CursoSeccion> moduloSections = sections.buscarPorModuloIdOrdenarPorPosicionAsc(moduloId);
        List<ContenidoItem> moduloItems = items.buscarPorModuloIdOrdenarPorPosicionAsc(moduloId);
        Map<UUID, String> seccionNames = new HashMap<>();
        for (CursoSeccion seccion : moduloSections) {
            seccionNames.put(seccion.getId(), seccion.getTitulo());
        }

        List<List<String>> table = new ArrayList<>();
        table.add(List.of("Sección", "Ítem", "Tipo", "Estado"));
        for (ContenidoItem item : moduloItems) {
            String seccionNombre = item.getSeccionId() == null ? "Sin sección"
                    : seccionNames.getOrDefault(item.getSeccionId(), "Sin sección");
            table.add(List.of(seccionNombre, value(item.getTitulo()), value(item.getOrigenTipo()),
                    item.isVisible() ? "Visible" : "Oculta"));
        }

        LocalDate today = LocalDate.now();
        String generatedAt = today.format(DATE);
        String nombreArchivo = "mundo-manitas-sesion-" + safeNombre(modulo.getTitulo())
                + "-" + today + ".pdf";
        byte[] bytes = pdf.writeDocument("Sesión: " + value(modulo.getTitulo()),
                value(cursoTitulo) + " · Temario de la sesión", "Ítems:", generatedAt, table);

        auditoria.record(actor, "EXPORT", "MODULO", moduloId, modulo.getTitulo());
        log.info("PDF de sesión generado módulo={} curso={} ítems={} actor={}",
                moduloId, modulo.getCursoId(), moduloItems.size(), actor);
        return new ResultadoExportacion(bytes, nombreArchivo, "application/pdf");
    }

    private String value(String text) {
        return text == null ? "" : text;
    }

    private String safeNombre(String titulo) {
        String simple = Normalizer.normalize(value(titulo), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
        String slug = simple.replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        return slug.isEmpty() ? "sin-titulo" : slug;
    }
}
