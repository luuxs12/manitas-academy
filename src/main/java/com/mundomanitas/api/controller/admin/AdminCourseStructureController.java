package com.mundomanitas.api.controller.admin;

import com.mundomanitas.api.dto.admin.AdminCourseForm;
import com.mundomanitas.api.dto.admin.AdminItemForm;
import com.mundomanitas.api.dto.admin.AdminOption;
import com.mundomanitas.api.dto.admin.AdminResourceForm;
import com.mundomanitas.api.dto.admin.AdminSectionForm;
import com.mundomanitas.api.dto.admin.AdminSyllabusForm;
import com.mundomanitas.api.security.AuthenticatedUser;
import com.mundomanitas.api.service.CourseContentService;
import com.mundomanitas.api.service.admin.AdminCatalogService;
import com.mundomanitas.api.service.admin.AdminCourseContentService;
import com.mundomanitas.api.service.admin.AdminNotificationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Carga del temario y el sílabo de un curso desde el panel administrativo. */
@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequestMapping("/admin/cursos/{courseId}/contenido")
public class AdminCourseStructureController extends AdminBaseController {

    private final AdminCatalogService catalog;
    private final AdminCourseContentService adminContent;
    private final CourseContentService content;

    public AdminCourseStructureController(AdminNotificationService notifications,
                                          AdminCatalogService catalog,
                                          AdminCourseContentService adminContent,
                                          CourseContentService content) {
        super(notifications);
        this.catalog = catalog;
        this.adminContent = adminContent;
        this.content = content;
    }

    @GetMapping
    public String structure(@PathVariable UUID courseId, Model model) {
        return render(courseId, model);
    }

    @PostMapping("/secciones")
    public String createSection(@PathVariable UUID courseId,
                                @Valid @ModelAttribute AdminSectionForm form,
                                BindingResult result,
                                @AuthenticationPrincipal AuthenticatedUser principal,
                                Model model,
                                RedirectAttributes redirect) {
        if (result.hasErrors()) return invalid(courseId, model);
        try {
            adminContent.createSection(form.moduleId(),
                    new AdminCourseContentService.SectionForm(form.title(), form.position(),
                            form.visible()), actorId(principal));
            redirect.addFlashAttribute("panelOk", "La sección se creó correctamente.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
        }
        return "redirect:" + structureUrl(courseId) + "#modulo-" + form.moduleId();
    }

    @PostMapping("/secciones/{sectionId}/mover")
    public String moveSection(@PathVariable UUID courseId, @PathVariable UUID sectionId,
                              @RequestParam boolean up,
                              @AuthenticationPrincipal AuthenticatedUser principal,
                              RedirectAttributes redirect) {
        try {
            adminContent.moveSection(sectionId, up, actorId(principal));
            redirect.addFlashAttribute("panelOk", "La sección se reordenó.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
        }
        return "redirect:" + structureUrl(courseId);
    }

    @GetMapping("/secciones/{sectionId}/eliminar")
    public String confirmDeleteSection(@PathVariable UUID courseId, @PathVariable UUID sectionId,
                                       Model model) {
        String title = findSectionTitle(courseId, sectionId);
        String base = structureUrl(courseId);
        return confirmation(model, "cursos", "Eliminar sección",
                "Los ítems de la sección quedarán como “Sin sección”; no se borrará su progreso.",
                base + "/secciones/" + sectionId + "/eliminar", "Eliminar sección",
                base, List.of("Sección: " + title));
    }

    @PostMapping("/secciones/{sectionId}/eliminar")
    public String deleteSection(@PathVariable UUID courseId, @PathVariable UUID sectionId,
                                @AuthenticationPrincipal AuthenticatedUser principal,
                                RedirectAttributes redirect) {
        try {
            adminContent.deleteSection(sectionId, actorId(principal));
            redirect.addFlashAttribute("panelOk", "La sección se eliminó.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
        }
        return "redirect:" + structureUrl(courseId);
    }

    @PostMapping("/items")
    public String createItem(@PathVariable UUID courseId,
                             @Valid @ModelAttribute AdminItemForm form,
                             BindingResult result,
                             @AuthenticationPrincipal AuthenticatedUser principal,
                             Model model,
                             RedirectAttributes redirect) {
        if (result.hasErrors()) return invalid(courseId, model);
        try {
            adminContent.createItem(new AdminCourseContentService.ItemForm(form.moduleId(),
                    form.sectionId(), form.type(), form.title(), form.description(), form.url(),
                    form.youtubeUrl(), form.durationSeconds(), form.position(), form.visible()),
                    actorId(principal));
            redirect.addFlashAttribute("panelOk", "El ítem se creó correctamente.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
        }
        return "redirect:" + structureUrl(courseId) + "#modulo-" + form.moduleId();
    }

    @PostMapping("/items/{itemId}/mover")
    public String moveItem(@PathVariable UUID courseId, @PathVariable UUID itemId,
                           @RequestParam boolean up,
                           @AuthenticationPrincipal AuthenticatedUser principal,
                           RedirectAttributes redirect) {
        try {
            adminContent.moveItem(itemId, up, actorId(principal));
            redirect.addFlashAttribute("panelOk", "El ítem se reordenó.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
        }
        return "redirect:" + structureUrl(courseId);
    }

    @GetMapping("/items/{itemId}/eliminar")
    public String confirmDeleteItem(@PathVariable UUID courseId, @PathVariable UUID itemId,
                                    Model model) {
        String title = findItemTitle(courseId, itemId);
        String base = structureUrl(courseId);
        return confirmation(model, "cursos", "Eliminar ítem",
                "Se borrará el contenido y su avance asociado. Esta acción no se puede deshacer.",
                base + "/items/" + itemId + "/eliminar", "Eliminar ítem",
                base, List.of("Ítem: " + title));
    }

    @PostMapping("/items/{itemId}/eliminar")
    public String deleteItem(@PathVariable UUID courseId, @PathVariable UUID itemId,
                             @AuthenticationPrincipal AuthenticatedUser principal,
                             RedirectAttributes redirect) {
        try {
            adminContent.deleteItem(itemId, actorId(principal));
            redirect.addFlashAttribute("panelOk", "El ítem se eliminó.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
        }
        return "redirect:" + structureUrl(courseId);
    }

    @PostMapping("/syllabus")
    public String saveSyllabus(@PathVariable UUID courseId,
                               @Valid @ModelAttribute AdminSyllabusForm form,
                               BindingResult result,
                               @AuthenticationPrincipal AuthenticatedUser principal,
                               Model model,
                               RedirectAttributes redirect) {
        if (result.hasErrors()) return invalid(courseId, model);
        try {
            List<AdminCourseContentService.ResourceForm> resources = new ArrayList<>();
            List<CourseContentService.ResourceView> current = content.syllabus(courseId).resources();
            for (int i = 0; i < current.size(); i++) {
                CourseContentService.ResourceView resource = current.get(i);
                resources.add(new AdminCourseContentService.ResourceForm(resource.id(),
                        resource.title(), resource.url(), i));
            }
            adminContent.saveSyllabus(courseId,
                    new AdminCourseContentService.SyllabusForm(form.objectives(),
                            form.requirements(), resources), actorId(principal));
            redirect.addFlashAttribute("panelOk", "El sílabo se guardó.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
        }
        return "redirect:" + structureUrl(courseId) + "#syllabus";
    }

    @PostMapping("/recursos")
    public String createResource(@PathVariable UUID courseId,
                                 @Valid @ModelAttribute AdminResourceForm form,
                                 BindingResult result,
                                 @AuthenticationPrincipal AuthenticatedUser principal,
                                 Model model,
                                 RedirectAttributes redirect) {
        if (result.hasErrors()) return invalid(courseId, model);
        try {
            adminContent.createResource(courseId, new AdminCourseContentService.ResourceForm(null,
                    form.title(), form.url(), form.position()), actorId(principal));
            redirect.addFlashAttribute("panelOk", "El recurso se agregó.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
        }
        return "redirect:" + structureUrl(courseId) + "#syllabus";
    }

    @PostMapping("/recursos/{resourceId}/eliminar")
    public String deleteResource(@PathVariable UUID courseId, @PathVariable UUID resourceId,
                                 @AuthenticationPrincipal AuthenticatedUser principal,
                                 RedirectAttributes redirect) {
        try {
            adminContent.deleteResource(resourceId, actorId(principal));
            redirect.addFlashAttribute("panelOk", "El recurso se quitó.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
        }
        return "redirect:" + structureUrl(courseId) + "#syllabus";
    }

    private String render(UUID courseId, Model model) {
        forms(model, load(courseId, model));
        return "admin/contenido";
    }

    /** Datos de solo lectura: se usan tanto al abrir como al volver por una validación. */
    private CourseContentService.Syllabus load(UUID courseId, Model model) {
        AdminCourseForm course = catalog.courseForm(courseId);
        CourseContentService.Syllabus syllabus = content.syllabus(courseId);
        CourseContentService.Outline outline = content.outline(courseId, null, null, true);
        common(model, "cursos", "Contenido de " + course.title());
        model.addAttribute("courseId", courseId);
        model.addAttribute("courseTitle", course.title());
        model.addAttribute("courseSlug", course.slug());
        model.addAttribute("outline", outline);
        model.addAttribute("syllabus", syllabus);
        List<AdminOption> modules = new ArrayList<>();
        List<AdminOption> sections = new ArrayList<>();
        sections.add(new AdminOption(null, "Sin sección"));
        int moduleNumber = 0;
        for (CourseContentService.ModuleView module : outline.modules()) {
            moduleNumber++;
            String label = "Módulo " + moduleNumber + " — " + module.title();
            modules.add(new AdminOption(module.id(), label));
            for (CourseContentService.SectionView section : module.sections()) {
                sections.add(new AdminOption(section.id(), label + " / " + section.title()));
            }
        }
        model.addAttribute("moduleOptions", modules);
        model.addAttribute("sectionOptions", sections);
        return syllabus;
    }

    /** Mantiene los formularios enlazados por Spring cuando hay errores de validación. */
    private void forms(Model model, CourseContentService.Syllabus syllabus) {
        addIfMissing(model, "sectionForm", "adminSectionForm",
                new AdminSectionForm(null, "", 0, true));
        addIfMissing(model, "itemForm", "adminItemForm",
                new AdminItemForm(null, null, "MATERIAL", "", "", "", null, null, 0, true));
        addIfMissing(model, "syllabusForm", "adminSyllabusForm",
                new AdminSyllabusForm(syllabus.objectives(), syllabus.requirements()));
        addIfMissing(model, "resourceForm", "adminResourceForm",
                new AdminResourceForm("", "", 0));
    }

    private void addIfMissing(Model model, String alias, String boundName, Object form) {
        if (!model.containsAttribute(boundName)) model.addAttribute(boundName, form);
        if (!model.containsAttribute(alias)) model.addAttribute(alias, model.asMap().get(boundName));
    }

    private String invalid(UUID courseId, Model model) {
        model.addAttribute("panelError", "Revisá los campos marcados.");
        forms(model, load(courseId, model));
        return "admin/contenido";
    }

    private String structureUrl(UUID courseId) {
        return "/admin/cursos/" + courseId + "/contenido";
    }

    private String findSectionTitle(UUID courseId, UUID sectionId) {
        return content.outline(courseId, null, null, true).modules().stream()
                .flatMap(module -> module.sections().stream())
                .filter(section -> section.id().equals(sectionId))
                .map(CourseContentService.SectionView::title)
                .findFirst().orElse("Sección");
    }

    private String findItemTitle(UUID courseId, UUID itemId) {
        CourseContentService.Outline outline = content.outline(courseId, null, null, true);
        return outline.modules().stream()
                .flatMap(module -> java.util.stream.Stream.concat(
                        module.sections().stream().flatMap(section -> section.items().stream()),
                        module.items().stream()))
                .filter(item -> item.id().equals(itemId))
                .map(CourseContentService.ItemView::title)
                .findFirst().orElse("Ítem");
    }
}
