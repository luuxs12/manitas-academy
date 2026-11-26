package com.mundomanitas.api.service.admin;

import com.mundomanitas.api.dto.admin.ExportResult;
import com.mundomanitas.api.model.ContentItem;
import com.mundomanitas.api.model.CourseModule;
import com.mundomanitas.api.model.CourseSection;
import com.mundomanitas.api.repository.ContentItemRepository;
import com.mundomanitas.api.repository.CourseModuleRepository;
import com.mundomanitas.api.repository.CourseRepository;
import com.mundomanitas.api.repository.CourseSectionRepository;
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
public class AdminSessionPdfServiceImpl implements AdminSessionPdfService {

    private static final Logger log = LoggerFactory.getLogger(AdminSessionPdfServiceImpl.class);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final CourseRepository courses;
    private final CourseModuleRepository modules;
    private final CourseSectionRepository sections;
    private final ContentItemRepository items;
    private final AdminAuditService audit;
    private final PlainPdfWriter pdf;

    public AdminSessionPdfServiceImpl(CourseRepository courses,
                                      CourseModuleRepository modules,
                                      CourseSectionRepository sections,
                                      ContentItemRepository items,
                                      AdminAuditService audit,
                                      PlainPdfWriter pdf) {
        this.courses = courses;
        this.modules = modules;
        this.sections = sections;
        this.items = items;
        this.audit = audit;
        this.pdf = pdf;
    }

    @Override
    @Transactional(readOnly = true)
    public ExportResult modulePdf(UUID moduleId, UUID actor) {
        CourseModule module = modules.findById(moduleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "El módulo solicitado no existe."));
        String courseTitle = courses.findById(module.getCourseId())
                .map(course -> course.getTitle())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "El curso de la sesión no existe."));

        List<CourseSection> moduleSections = sections.findByModuleIdOrderByPositionAsc(moduleId);
        List<ContentItem> moduleItems = items.findByModuleIdOrderByPositionAsc(moduleId);
        Map<UUID, String> sectionNames = new HashMap<>();
        for (CourseSection section : moduleSections) {
            sectionNames.put(section.getId(), section.getTitle());
        }

        List<List<String>> table = new ArrayList<>();
        table.add(List.of("Sección", "Ítem", "Tipo", "Estado"));
        for (ContentItem item : moduleItems) {
            String sectionName = item.getSectionId() == null ? "Sin sección"
                    : sectionNames.getOrDefault(item.getSectionId(), "Sin sección");
            table.add(List.of(sectionName, value(item.getTitle()), value(item.getSourceType()),
                    item.isVisible() ? "Visible" : "Oculta"));
        }

        LocalDate today = LocalDate.now();
        String generatedAt = today.format(DATE);
        String fileName = "mundo-manitas-sesion-" + safeName(module.getTitle())
                + "-" + today + ".pdf";
        byte[] bytes = pdf.writeDocument("Sesión: " + value(module.getTitle()),
                value(courseTitle) + " · Temario de la sesión", "Ítems:", generatedAt, table);

        audit.record(actor, "EXPORT", "MODULO", moduleId, module.getTitle());
        log.info("PDF de sesión generado módulo={} curso={} ítems={} actor={}",
                moduleId, module.getCourseId(), moduleItems.size(), actor);
        return new ExportResult(bytes, fileName, "application/pdf");
    }

    private String value(String text) {
        return text == null ? "" : text;
    }

    private String safeName(String title) {
        String plain = Normalizer.normalize(value(title), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
        String slug = plain.replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        return slug.isEmpty() ? "sin-titulo" : slug;
    }
}
