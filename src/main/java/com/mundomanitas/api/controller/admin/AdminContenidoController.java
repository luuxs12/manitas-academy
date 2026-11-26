package com.mundomanitas.api.controller.admin;

import com.mundomanitas.api.dto.admin.AdminEjercicioFormulario;
import com.mundomanitas.api.dto.admin.AdminModuloFormulario;
import com.mundomanitas.api.dto.admin.AdminVideoFormulario;
import com.mundomanitas.api.security.AutenticadoUsuario;
import com.mundomanitas.api.service.admin.AdminCatalogService;
import com.mundomanitas.api.service.admin.AdminNotificacionService;
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
public class AdminContenidoController extends AdminBaseController {

    private final AdminCatalogService catalog;

    public AdminContenidoController(AdminNotificacionService notifications, AdminCatalogService catalog) {
        super(notifications);
        this.catalog = catalog;
    }

    /* ------------------------------ módulos ------------------------------ */

    @GetMapping("/modulos")
    public String modules(@RequestParam(name = "q", required = false) String query,
                          @RequestParam(name = "curso", required = false) UUID cursoId,
                          @RequestParam(name = "page", defaultValue = "0") int page,
                          Model model) {
        common(model, "modulos", "Módulos");
        model.addAttribute("page", catalog.modules(cursoId, query,
                PageRequest.of(Math.max(page, 0), 10)));
        model.addAttribute("query", query);
        model.addAttribute("cursoId", cursoId);
        model.addAttribute("options", catalog.cursoOptions());
        return "admin/modulos";
    }

    @GetMapping("/modulos/nuevo")
    public String newModulo(Model model) {
        common(model, "modulos", "Nuevo módulo");
        model.addAttribute("adminModuloFormulario", new AdminModuloFormulario(null, "", 0));
        model.addAttribute("options", catalog.cursoOptions());
        return "admin/modulo-form";
    }

    @PostMapping("/modulos")
    public String createModulo(@Valid @ModelAttribute AdminModuloFormulario formulario,
                               BindingResult resultado,
                               @AuthenticationPrincipal AutenticadoUsuario principal,
                               Model model,
                               RedirectAttributes redirect) {
        if (resultado.hasErrors()) return moduloFormulario(model, formulario, "Nuevo módulo", null);
        try {
            UUID id = catalog.createModulo(formulario, principal.getId());
            redirect.addFlashAttribute("panelOk", "El módulo se creó correctamente.");
            return "redirect:/admin/modulos/" + id + "/editar";
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
            return "redirect:/admin/modulos/nuevo";
        }
    }

    @GetMapping("/modulos/{id}/editar")
    public String editModulo(@PathVariable UUID id, Model model, RedirectAttributes redirect) {
        try {
            common(model, "modulos", "Editar módulo");
            model.addAttribute("adminModuloFormulario", catalog.moduloFormulario(id));
            model.addAttribute("itemId", id);
            model.addAttribute("options", catalog.cursoOptions());
            return "admin/modulo-form";
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
            return "redirect:/admin/modulos";
        }
    }

    @PostMapping("/modulos/{id}")
    public String updateModulo(@PathVariable UUID id,
                               @Valid @ModelAttribute AdminModuloFormulario formulario,
                               BindingResult resultado,
                               @AuthenticationPrincipal AutenticadoUsuario principal,
                               Model model,
                               RedirectAttributes redirect) {
        if (resultado.hasErrors()) return moduloFormulario(model, formulario, "Editar módulo", id);
        try {
            catalog.updateModulo(id, formulario, principal.getId());
            redirect.addFlashAttribute("panelOk", "El módulo se guardó.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
        }
        return "redirect:/admin/modulos/" + id + "/editar";
    }

    @PostMapping("/modulos/{id}/mover")
    public String moveModulo(@PathVariable UUID id,
                             @RequestParam boolean up,
                             @AuthenticationPrincipal AutenticadoUsuario principal,
                             RedirectAttributes redirect) {
        try {
            catalog.moveModulo(id, up, principal.getId());
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
        }
        return "redirect:/admin/modulos";
    }

    @GetMapping("/modulos/{id}/eliminar")
    public String confirmDeleteModulo(@PathVariable UUID id, Model model,
                                      RedirectAttributes redirect) {
        try {
            String titulo = catalog.moduloFormulario(id).titulo();
            return confirmation(model, "modulos", "Eliminar módulo",
                    "Se borrará el módulo y su contenido. Esta acción no se puede deshacer.",
                    "/admin/modulos/" + id + "/eliminar", "Eliminar módulo",
                    "/admin/modulos", List.of("Módulo: " + titulo));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
            return "redirect:/admin/modulos";
        }
    }

    @PostMapping("/modulos/{id}/eliminar")
    public String deleteModulo(@PathVariable UUID id,
                               @AuthenticationPrincipal AutenticadoUsuario principal,
                               RedirectAttributes redirect) {
        try {
            catalog.deleteModulo(id, principal.getId());
            redirect.addFlashAttribute("panelOk", "El módulo se eliminó.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
        }
        return "redirect:/admin/modulos";
    }

    /* ------------------------------- videos ------------------------------ */

    @GetMapping("/videos")
    public String videos(@RequestParam(name = "q", required = false) String query,
                         @RequestParam(name = "curso", required = false) UUID cursoId,
                         @RequestParam(name = "modulo", required = false) UUID moduloId,
                         @RequestParam(name = "page", defaultValue = "0") int page,
                         Model model) {
        common(model, "videos", "Videos");
        var pagina = PageRequest.of(Math.max(page, 0), 10);
        model.addAttribute("page", cursoId == null
                ? catalog.videos(moduloId, query, pagina)
                : catalog.videos(cursoId, moduloId, query, pagina));
        model.addAttribute("query", query);
        model.addAttribute("cursoId", cursoId);
        model.addAttribute("moduloId", moduloId);
        model.addAttribute("options", catalog.moduloOptions());
        model.addAttribute("cursoOptions", catalog.cursoOptions());
        return "admin/videos";
    }

    @GetMapping("/videos/nuevo")
    public String newVideo(Model model) {
        common(model, "videos", "Nuevo video");
        model.addAttribute("adminVideoFormulario", new AdminVideoFormulario(null, "", 0, ""));
        model.addAttribute("options", catalog.moduloOptions());
        model.addAttribute("cursoOptions", catalog.cursoOptions());
        return "admin/video-form";
    }

    @PostMapping("/videos")
    public String createVideo(@Valid @ModelAttribute AdminVideoFormulario formulario,
                              BindingResult resultado,
                              @AuthenticationPrincipal AutenticadoUsuario principal,
                              Model model,
                              RedirectAttributes redirect) {
        if (resultado.hasErrors()) return videoFormulario(model, formulario, "Nuevo video", null);
        try {
            UUID id = catalog.createVideo(formulario, principal.getId());
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
            model.addAttribute("adminVideoFormulario", catalog.videoFormulario(id));
            model.addAttribute("itemId", id);
            model.addAttribute("options", catalog.moduloOptions());
            model.addAttribute("cursoOptions", catalog.cursoOptions());
            return "admin/video-form";
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
            return "redirect:/admin/videos";
        }
    }

    @PostMapping("/videos/{id}")
    public String updateVideo(@PathVariable UUID id,
                              @Valid @ModelAttribute AdminVideoFormulario formulario,
                              BindingResult resultado,
                              @AuthenticationPrincipal AutenticadoUsuario principal,
                              Model model,
                              RedirectAttributes redirect) {
        if (resultado.hasErrors()) return videoFormulario(model, formulario, "Editar video", id);
        try {
            catalog.updateVideo(id, formulario, principal.getId());
            redirect.addFlashAttribute("panelOk", "El video se guardó.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
        }
        return "redirect:/admin/videos/" + id + "/editar";
    }

    @PostMapping("/videos/{id}/mover")
    public String moveVideo(@PathVariable UUID id,
                            @RequestParam boolean up,
                            @AuthenticationPrincipal AutenticadoUsuario principal,
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
            AdminVideoFormulario formulario = catalog.videoFormulario(id);
            return confirmation(model, "videos", "Eliminar video",
                    "Se borrará la clase y dejará de contar como lección.",
                    "/admin/videos/" + id + "/eliminar", "Eliminar video",
                    "/admin/videos", List.of("Clase: " + formulario.titulo(),
                    "YouTube: " + formulario.youtubeUrl()));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
            return "redirect:/admin/videos";
        }
    }

    @PostMapping("/videos/{id}/eliminar")
    public String deleteVideo(@PathVariable UUID id,
                              @AuthenticationPrincipal AutenticadoUsuario principal,
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
                            @RequestParam(name = "curso", required = false) UUID cursoId,
                            @RequestParam(name = "modulo", required = false) UUID moduloId,
                            @RequestParam(name = "page", defaultValue = "0") int page,
                            Model model) {
        common(model, "ejercicios", "Ejercicios");
        var pagina = PageRequest.of(Math.max(page, 0), 10);
        model.addAttribute("page", cursoId == null
                ? catalog.exercises(moduloId, query, pagina)
                : catalog.exercises(cursoId, moduloId, query, pagina));
        model.addAttribute("query", query);
        model.addAttribute("cursoId", cursoId);
        model.addAttribute("moduloId", moduloId);
        model.addAttribute("options", catalog.moduloOptions());
        model.addAttribute("cursoOptions", catalog.cursoOptions());
        return "admin/ejercicios";
    }

    @GetMapping("/ejercicios/nuevo")
    public String newEjercicio(Model model) {
        common(model, "ejercicios", "Nuevo ejercicio");
        model.addAttribute("adminEjercicioFormulario", new AdminEjercicioFormulario(null, "", "", 0));
        model.addAttribute("options", catalog.moduloOptions());
        model.addAttribute("cursoOptions", catalog.cursoOptions());
        return "admin/ejercicio-form";
    }

    @PostMapping("/ejercicios")
    public String createEjercicio(@Valid @ModelAttribute AdminEjercicioFormulario formulario,
                                 BindingResult resultado,
                                 @AuthenticationPrincipal AutenticadoUsuario principal,
                                 Model model,
                                 RedirectAttributes redirect) {
        if (resultado.hasErrors()) return ejercicioFormulario(model, formulario, "Nuevo ejercicio", null);
        try {
            UUID id = catalog.createEjercicio(formulario, principal.getId());
            redirect.addFlashAttribute("panelOk", "El ejercicio se creó correctamente.");
            return "redirect:/admin/ejercicios/" + id + "/editar";
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
            return "redirect:/admin/ejercicios/nuevo";
        }
    }

    @GetMapping("/ejercicios/{id}/editar")
    public String editEjercicio(@PathVariable UUID id, Model model, RedirectAttributes redirect) {
        try {
            common(model, "ejercicios", "Editar ejercicio");
            model.addAttribute("adminEjercicioFormulario", catalog.ejercicioFormulario(id));
            model.addAttribute("preguntasCount", catalog.contarPreguntas(id));
            model.addAttribute("itemId", id);
            model.addAttribute("options", catalog.moduloOptions());
            model.addAttribute("cursoOptions", catalog.cursoOptions());
            return "admin/ejercicio-form";
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
            return "redirect:/admin/ejercicios";
        }
    }

    @PostMapping("/ejercicios/{id}")
    public String updateEjercicio(@PathVariable UUID id,
                                 @Valid @ModelAttribute AdminEjercicioFormulario formulario,
                                 BindingResult resultado,
                                 @AuthenticationPrincipal AutenticadoUsuario principal,
                                 Model model,
                                 RedirectAttributes redirect) {
        if (resultado.hasErrors()) return ejercicioFormulario(model, formulario, "Editar ejercicio", id);
        try {
            catalog.updateEjercicio(id, formulario, principal.getId());
            redirect.addFlashAttribute("panelOk", "El ejercicio se guardó.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
        }
        return "redirect:/admin/ejercicios/" + id + "/editar";
    }

    @GetMapping("/ejercicios/{id}/eliminar")
    public String confirmDeleteEjercicio(@PathVariable UUID id, Model model,
                                        RedirectAttributes redirect) {
        try {
            AdminEjercicioFormulario formulario = catalog.ejercicioFormulario(id);
            return confirmation(model, "ejercicios", "Eliminar ejercicio",
                    "Se borrará la consigna de este módulo.",
                    "/admin/ejercicios/" + id + "/eliminar", "Eliminar ejercicio",
                    "/admin/ejercicios", List.of("Ejercicio: " + formulario.titulo()));
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
            return "redirect:/admin/ejercicios";
        }
    }

    @PostMapping("/ejercicios/{id}/eliminar")
    public String deleteEjercicio(@PathVariable UUID id,
                                 @AuthenticationPrincipal AutenticadoUsuario principal,
                                 RedirectAttributes redirect) {
        try {
            catalog.deleteEjercicio(id, principal.getId());
            redirect.addFlashAttribute("panelOk", "El ejercicio se eliminó.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
        }
        return "redirect:/admin/ejercicios";
    }

    private String moduloFormulario(Model model, AdminModuloFormulario formulario, String titulo, UUID id) {
        common(model, "modulos", titulo);
        model.addAttribute("adminModuloFormulario", formulario);
        if (id != null) model.addAttribute("itemId", id);
        model.addAttribute("options", catalog.cursoOptions());
        return "admin/modulo-form";
    }

    private String videoFormulario(Model model, AdminVideoFormulario formulario, String titulo, UUID id) {
        common(model, "videos", titulo);
        model.addAttribute("adminVideoFormulario", formulario);
        if (id != null) model.addAttribute("itemId", id);
        model.addAttribute("options", catalog.moduloOptions());
        model.addAttribute("cursoOptions", catalog.cursoOptions());
        return "admin/video-form";
    }

    private String ejercicioFormulario(Model model, AdminEjercicioFormulario formulario, String titulo, UUID id) {
        common(model, "ejercicios", titulo);
        model.addAttribute("adminEjercicioFormulario", formulario);
        if (id != null) {
            model.addAttribute("itemId", id);
            model.addAttribute("preguntasCount", catalog.contarPreguntas(id));
        }
        model.addAttribute("options", catalog.moduloOptions());
        model.addAttribute("cursoOptions", catalog.cursoOptions());
        return "admin/ejercicio-form";
    }
}
