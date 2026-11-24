package com.mundomanitas.api.controller.admin;

import com.mundomanitas.api.dto.admin.AdminExerciseForm;
import com.mundomanitas.api.dto.admin.AdminModuleForm;
import com.mundomanitas.api.dto.admin.AdminVideoForm;
import com.mundomanitas.api.security.AuthenticatedUser;
import com.mundomanitas.api.service.admin.AdminCatalogService;
import com.mundomanitas.api.service.admin.AdminNotificationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
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

import java.util.List;
import java.util.UUID;

/** Gestión de módulos, videos y ejercicios del catálogo. */
@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequestMapping("/admin")
public class AdminContentController extends AdminBaseController {

    private final AdminCatalogService catalog;

    public AdminContentController(AdminNotificationService notifications, AdminCatalogService catalog) {
        super(notifications);
        this.catalog = catalog;
    }

    /* ------------------------------ módulos ------------------------------ */

    @GetMapping("/modulos")
    public String modules(@RequestParam(name = "q", required = false) String query,
                          @RequestParam(name = "curso", required = false) UUID courseId,
                          @RequestParam(name = "page", defaultValue = "0") int page,
                          Model model) {
        common(model, "modulos", "Módulos");
        model.addAttribute("page", catalog.modules(courseId, query,
                PageRequest.of(Math.max(page, 0), 10)));
        model.addAttribute("query", query);
        model.addAttribute("courseId", courseId);
        model.addAttribute("options", catalog.courseOptions());
        return "admin/modulos";
    }

    @GetMapping("/modulos/nuevo")
    public String newModule(Model model) {
        common(model, "modulos", "Nuevo módulo");
        model.addAttribute("adminModuleForm", new AdminModuleForm(null, "", 0));
        model.addAttribute("options", catalog.courseOptions());
        return "admin/modulo-form";
    }

    @PostMapping("/modulos")
    public String createModule(@Valid @ModelAttribute AdminModuleForm form,
                               BindingResult result,
                               @AuthenticationPrincipal AuthenticatedUser principal,
                               Model model,
                               RedirectAttributes redirect) {
        if (result.hasErrors()) return moduleForm(model, form, "Nuevo módulo", null);
        try {
            UUID id = catalog.createModule(form, principal.getId());
            redirect.addFlashAttribute("panelOk", "El módulo se creó correctamente.");
            return "redirect:/admin/modulos/" + id + "/editar";
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
            return "redirect:/admin/modulos/nuevo";
        }
    }

    @GetMapping("/modulos/{id}/editar")
    public String editModule(@PathVariable UUID id, Model model, RedirectAttributes redirect) {
        try {
            common(model, "modulos", "Editar módulo");
            model.addAttribute("adminModuleForm", catalog.moduleForm(id));
            model.addAttribute("itemId", id);
            model.addAttribute("options", catalog.courseOptions());
            return "admin/modulo-form";
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
            return "redirect:/admin/modulos";
        }
    }

    @PostMapping("/modulos/{id}")
    public String updateModule(@PathVariable UUID id,
                               @Valid @ModelAttribute AdminModuleForm form,
                               BindingResult result,
                               @AuthenticationPrincipal AuthenticatedUser principal,
                               Model model,
                               RedirectAttributes redirect) {
        if (result.hasErrors()) return moduleForm(model, form, "Editar módulo", id);
        try {
            catalog.updateModule(id, form, principal.getId());
            redirect.addFlashAttribute("panelOk", "El módulo se guardó.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
        }
        return "redirect:/admin/modulos/" + id + "/editar";
    }

    @PostMapping("/modulos/{id}/mover")
    public String moveModule(@PathVariable UUID id,
                             @RequestParam boolean up,
                             @AuthenticationPrincipal AuthenticatedUser principal,
                             RedirectAttributes redirect) {
        try {
            catalog.moveModule(id, up, principal.getId());
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
        }
        return "redirect:/admin/modulos";
    }

    @GetMapping("/modulos/{id}/eliminar")
    public String confirmDeleteModule(@PathVariable UUID id, Model model,
                                      RedirectAttributes redirect) {
        try {
            String title = catalog.moduleForm(id).title();
            return confirmation(model, "modulos", "Eliminar módulo",
                    "Se borrará el módulo y su contenido. Esta acción no se puede deshacer.",
                    "/admin/modulos/" + id + "/eliminar", "Eliminar módulo",
                    "/admin/modulos", List.of("Módulo: " + title));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
            return "redirect:/admin/modulos";
        }
    }

    @PostMapping("/modulos/{id}/eliminar")
    public String deleteModule(@PathVariable UUID id,
                               @AuthenticationPrincipal AuthenticatedUser principal,
                               RedirectAttributes redirect) {
        try {
            catalog.deleteModule(id, principal.getId());
            redirect.addFlashAttribute("panelOk", "El módulo se eliminó.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
        }
        return "redirect:/admin/modulos";
    }

    /* ------------------------------- videos ------------------------------ */

    @GetMapping("/videos")
    public String videos(@RequestParam(name = "q", required = false) String query,
                         @RequestParam(name = "modulo", required = false) UUID moduleId,
                         @RequestParam(name = "page", defaultValue = "0") int page,
                         Model model) {
        common(model, "videos", "Videos");
        model.addAttribute("page", catalog.videos(moduleId, query,
                PageRequest.of(Math.max(page, 0), 10)));
        model.addAttribute("query", query);
        model.addAttribute("moduleId", moduleId);
        model.addAttribute("options", catalog.moduleOptions());
        return "admin/videos";
    }

    @GetMapping("/videos/nuevo")
    public String newVideo(Model model) {
        common(model, "videos", "Nuevo video");
        model.addAttribute("adminVideoForm", new AdminVideoForm(null, "", 0, ""));
        model.addAttribute("options", catalog.moduleOptions());
        return "admin/video-form";
    }

    @PostMapping("/videos")
    public String createVideo(@Valid @ModelAttribute AdminVideoForm form,
                              BindingResult result,
                              @AuthenticationPrincipal AuthenticatedUser principal,
                              Model model,
                              RedirectAttributes redirect) {
        if (result.hasErrors()) return videoForm(model, form, "Nuevo video", null);
        try {
            UUID id = catalog.createVideo(form, principal.getId());
            redirect.addFlashAttribute("panelOk", "El video se creó correctamente.");
            return "redirect:/admin/videos/" + id + "/editar";
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
            return "redirect:/admin/videos/nuevo";
        }
    }

    @GetMapping("/videos/{id}/editar")
    public String editVideo(@PathVariable UUID id, Model model, RedirectAttributes redirect) {
        try {
            common(model, "videos", "Editar video");
            model.addAttribute("adminVideoForm", catalog.videoForm(id));
            model.addAttribute("itemId", id);
            model.addAttribute("options", catalog.moduleOptions());
            return "admin/video-form";
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
            return "redirect:/admin/videos";
        }
    }

    @PostMapping("/videos/{id}")
    public String updateVideo(@PathVariable UUID id,
                              @Valid @ModelAttribute AdminVideoForm form,
                              BindingResult result,
                              @AuthenticationPrincipal AuthenticatedUser principal,
                              Model model,
                              RedirectAttributes redirect) {
        if (result.hasErrors()) return videoForm(model, form, "Editar video", id);
        try {
            catalog.updateVideo(id, form, principal.getId());
            redirect.addFlashAttribute("panelOk", "El video se guardó.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
        }
        return "redirect:/admin/videos/" + id + "/editar";
    }

    @PostMapping("/videos/{id}/mover")
    public String moveVideo(@PathVariable UUID id,
                            @RequestParam boolean up,
                            @AuthenticationPrincipal AuthenticatedUser principal,
                            RedirectAttributes redirect) {
        try {
            catalog.moveVideo(id, up, principal.getId());
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
        }
        return "redirect:/admin/videos";
    }

    @GetMapping("/videos/{id}/eliminar")
    public String confirmDeleteVideo(@PathVariable UUID id, Model model,
                                     RedirectAttributes redirect) {
        try {
            AdminVideoForm form = catalog.videoForm(id);
            return confirmation(model, "videos", "Eliminar video",
                    "Se borrará la clase y dejará de contar como lección.",
                    "/admin/videos/" + id + "/eliminar", "Eliminar video",
                    "/admin/videos", List.of("Clase: " + form.title(),
                    "YouTube: " + form.youtubeUrl()));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
            return "redirect:/admin/videos";
        }
    }

    @PostMapping("/videos/{id}/eliminar")
    public String deleteVideo(@PathVariable UUID id,
                              @AuthenticationPrincipal AuthenticatedUser principal,
                              RedirectAttributes redirect) {
        try {
            catalog.deleteVideo(id, principal.getId());
            redirect.addFlashAttribute("panelOk", "El video se eliminó.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
        }
        return "redirect:/admin/videos";
    }

    /* ----------------------------- ejercicios ---------------------------- */

    @GetMapping("/ejercicios")
    public String exercises(@RequestParam(name = "q", required = false) String query,
                            @RequestParam(name = "modulo", required = false) UUID moduleId,
                            @RequestParam(name = "page", defaultValue = "0") int page,
                            Model model) {
        common(model, "ejercicios", "Ejercicios");
        model.addAttribute("page", catalog.exercises(moduleId, query,
                PageRequest.of(Math.max(page, 0), 10)));
        model.addAttribute("query", query);
        model.addAttribute("moduleId", moduleId);
        model.addAttribute("options", catalog.moduleOptions());
        return "admin/ejercicios";
    }

    @GetMapping("/ejercicios/nuevo")
    public String newExercise(Model model) {
        common(model, "ejercicios", "Nuevo ejercicio");
        model.addAttribute("adminExerciseForm", new AdminExerciseForm(null, "", "", 0));
        model.addAttribute("options", catalog.moduleOptions());
        return "admin/ejercicio-form";
    }

    @PostMapping("/ejercicios")
    public String createExercise(@Valid @ModelAttribute AdminExerciseForm form,
                                 BindingResult result,
                                 @AuthenticationPrincipal AuthenticatedUser principal,
                                 Model model,
                                 RedirectAttributes redirect) {
        if (result.hasErrors()) return exerciseForm(model, form, "Nuevo ejercicio", null);
        try {
            UUID id = catalog.createExercise(form, principal.getId());
            redirect.addFlashAttribute("panelOk", "El ejercicio se creó correctamente.");
            return "redirect:/admin/ejercicios/" + id + "/editar";
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
            return "redirect:/admin/ejercicios/nuevo";
        }
    }

    @GetMapping("/ejercicios/{id}/editar")
    public String editExercise(@PathVariable UUID id, Model model, RedirectAttributes redirect) {
        try {
            common(model, "ejercicios", "Editar ejercicio");
            model.addAttribute("adminExerciseForm", catalog.exerciseForm(id));
            model.addAttribute("itemId", id);
            model.addAttribute("options", catalog.moduleOptions());
            return "admin/ejercicio-form";
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
            return "redirect:/admin/ejercicios";
        }
    }

    @PostMapping("/ejercicios/{id}")
    public String updateExercise(@PathVariable UUID id,
                                 @Valid @ModelAttribute AdminExerciseForm form,
                                 BindingResult result,
                                 @AuthenticationPrincipal AuthenticatedUser principal,
                                 Model model,
                                 RedirectAttributes redirect) {
        if (result.hasErrors()) return exerciseForm(model, form, "Editar ejercicio", id);
        try {
            catalog.updateExercise(id, form, principal.getId());
            redirect.addFlashAttribute("panelOk", "El ejercicio se guardó.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
        }
        return "redirect:/admin/ejercicios/" + id + "/editar";
    }

    @GetMapping("/ejercicios/{id}/eliminar")
    public String confirmDeleteExercise(@PathVariable UUID id, Model model,
                                        RedirectAttributes redirect) {
        try {
            AdminExerciseForm form = catalog.exerciseForm(id);
            return confirmation(model, "ejercicios", "Eliminar ejercicio",
                    "Se borrará la consigna de este módulo.",
                    "/admin/ejercicios/" + id + "/eliminar", "Eliminar ejercicio",
                    "/admin/ejercicios", List.of("Ejercicio: " + form.title()));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
            return "redirect:/admin/ejercicios";
        }
    }

    @PostMapping("/ejercicios/{id}/eliminar")
    public String deleteExercise(@PathVariable UUID id,
                                 @AuthenticationPrincipal AuthenticatedUser principal,
                                 RedirectAttributes redirect) {
        try {
            catalog.deleteExercise(id, principal.getId());
            redirect.addFlashAttribute("panelOk", "El ejercicio se eliminó.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
        }
        return "redirect:/admin/ejercicios";
    }

    private String moduleForm(Model model, AdminModuleForm form, String title, UUID id) {
        common(model, "modulos", title);
        model.addAttribute("adminModuleForm", form);
        if (id != null) model.addAttribute("itemId", id);
        model.addAttribute("options", catalog.courseOptions());
        return "admin/modulo-form";
    }

    private String videoForm(Model model, AdminVideoForm form, String title, UUID id) {
        common(model, "videos", title);
        model.addAttribute("adminVideoForm", form);
        if (id != null) model.addAttribute("itemId", id);
        model.addAttribute("options", catalog.moduleOptions());
        return "admin/video-form";
    }

    private String exerciseForm(Model model, AdminExerciseForm form, String title, UUID id) {
        common(model, "ejercicios", title);
        model.addAttribute("adminExerciseForm", form);
        if (id != null) model.addAttribute("itemId", id);
        model.addAttribute("options", catalog.moduleOptions());
        return "admin/ejercicio-form";
    }
}
