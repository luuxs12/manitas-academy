package com.mundomanitas.api.controller.admin;

import com.mundomanitas.api.dto.admin.AdminCursoFormulario;
import com.mundomanitas.api.dto.admin.AdminDocumentFormulario;
import com.mundomanitas.api.dto.admin.AdminItemFormulario;
import com.mundomanitas.api.dto.admin.AdminOpcion;
import com.mundomanitas.api.dto.admin.AdminRecursoFormulario;
import com.mundomanitas.api.dto.admin.AdminSeccionFormulario;
import com.mundomanitas.api.dto.admin.AdminSilaboFormulario;
import com.mundomanitas.api.security.AutenticadoUsuario;
import com.mundomanitas.api.service.CursoContenidoService;
import com.mundomanitas.api.service.admin.AdminCatalogService;
import com.mundomanitas.api.service.admin.AdminCursoContenidoService;
import com.mundomanitas.api.service.admin.AdminNotificacionService;
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
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Carga del temario y el sílabo de un curso desde el panel administrativo. */
@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequestMapping("/admin/cursos/{cursoId}/contenido")
public class AdminCursoEstructuraController extends AdminBaseController {

    private final AdminCatalogService catalog;
    private final AdminCursoContenidoService adminContenido;
    private final CursoContenidoService content;

    public AdminCursoEstructuraController(AdminNotificacionService notifications,
                                          AdminCatalogService catalog,
                                          AdminCursoContenidoService adminContenido,
                                          CursoContenidoService content) {
        super(notifications);
        this.catalog = catalog;
        this.adminContenido = adminContenido;
        this.content = content;
    }

    @GetMapping
    public String estructura(@PathVariable UUID cursoId, Model model) {
        return render(cursoId, model);
    }

    @PostMapping("/secciones")
    public String createSeccion(@PathVariable UUID cursoId,
                                @Valid @ModelAttribute AdminSeccionFormulario formulario,
                                BindingResult resultado,
                                @AuthenticationPrincipal AutenticadoUsuario principal,
                                Model model,
                                RedirectAttributes redirect) {
        if (resultado.hasErrors()) return invalido(cursoId, model);
        try {
            adminContenido.createSeccion(formulario.moduloId(),
                    new AdminCursoContenidoService.SeccionFormulario(formulario.titulo(), formulario.posicion(),
                            formulario.visible()), actorId(principal));
            redirect.addFlashAttribute("panelOk", "La sección se creó correctamente.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
        }
        return "redirect:" + estructuraUrl(cursoId) + "#modulo-" + formulario.moduloId();
    }

    @PostMapping("/secciones/{seccionId}/mover")
    public String moveSeccion(@PathVariable UUID cursoId, @PathVariable UUID seccionId,
                              @RequestParam boolean up,
                              @AuthenticationPrincipal AutenticadoUsuario principal,
                              RedirectAttributes redirect) {
        try {
            adminContenido.moveSeccion(seccionId, up, actorId(principal));
            redirect.addFlashAttribute("panelOk", "La sección se reordenó.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
        }
        return "redirect:" + estructuraUrl(cursoId);
    }

    @GetMapping("/secciones/{seccionId}/eliminar")
    public String confirmDeleteSeccion(@PathVariable UUID cursoId, @PathVariable UUID seccionId,
                                       Model model) {
        String titulo = buscarSeccionTitulo(cursoId, seccionId);
        String base = estructuraUrl(cursoId);
        return confirmation(model, "cursos", "Eliminar sección",
                "Los ítems de la sección quedarán como “Sin sección”; no se borrará su progreso.",
                base + "/secciones/" + seccionId + "/eliminar", "Eliminar sección",
                base, List.of("Sección: " + titulo));
    }

    @PostMapping("/secciones/{seccionId}/eliminar")
    public String deleteSeccion(@PathVariable UUID cursoId, @PathVariable UUID seccionId,
                                @AuthenticationPrincipal AutenticadoUsuario principal,
                                RedirectAttributes redirect) {
        try {
            adminContenido.deleteSeccion(seccionId, actorId(principal));
            redirect.addFlashAttribute("panelOk", "La sección se eliminó.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
        }
        return "redirect:" + estructuraUrl(cursoId);
    }

    @PostMapping("/items")
    public String createItem(@PathVariable UUID cursoId,
                             @Valid @ModelAttribute AdminItemFormulario formulario,
                             BindingResult resultado,
                             @AuthenticationPrincipal AutenticadoUsuario principal,
                             Model model,
                             RedirectAttributes redirect) {
        if (resultado.hasErrors()) return invalido(cursoId, model);
        try {
            adminContenido.createItem(new AdminCursoContenidoService.ItemFormulario(formulario.moduloId(),
                    formulario.seccionId(), formulario.type(), formulario.titulo(), formulario.descripcion(), formulario.url(),
                    formulario.youtubeUrl(), formulario.duracionSegundos(), formulario.posicion(), formulario.visible()),
                    actorId(principal));
            redirect.addFlashAttribute("panelOk", "El ítem se creó correctamente.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
        }
        return "redirect:" + estructuraUrl(cursoId) + "#modulo-" + formulario.moduloId();
    }

    @PostMapping("/documentos")
    public String createDocument(@PathVariable UUID cursoId,
                                 @Valid @ModelAttribute AdminDocumentFormulario formulario,
                                 BindingResult resultado,
                                 @AuthenticationPrincipal AutenticadoUsuario principal,
                                 Model model,
                                 RedirectAttributes redirect) {
        if (resultado.hasErrors()) return invalido(cursoId, model);
        try {
            adminContenido.createDocument(new AdminCursoContenidoService.DocumentFormulario(
                    formulario.moduloId(), formulario.seccionId(), formulario.titulo(), formulario.descripcion(),
                    formulario.file(), null, formulario.posicion(), formulario.visible()), actorId(principal));
            redirect.addFlashAttribute("panelOk", "El documento se subió correctamente.");
        } catch (IllegalArgumentException | ResponseStatusException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
        }
        return "redirect:" + estructuraUrl(cursoId) + "#modulo-" + formulario.moduloId();
    }

    @PostMapping("/items/{itemId}/mover")
    public String moveItem(@PathVariable UUID cursoId, @PathVariable UUID itemId,
                           @RequestParam boolean up,
                           @AuthenticationPrincipal AutenticadoUsuario principal,
                           RedirectAttributes redirect) {
        try {
            adminContenido.moveItem(itemId, up, actorId(principal));
            redirect.addFlashAttribute("panelOk", "El ítem se reordenó.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
        }
        return "redirect:" + estructuraUrl(cursoId);
    }

    @PostMapping("/modulos/{moduloId}/visible")
    public String toggleModuloVisible(@PathVariable UUID cursoId, @PathVariable UUID moduloId,
                                      @AuthenticationPrincipal AutenticadoUsuario principal,
                                      RedirectAttributes redirect) {
        try {
            adminContenido.toggleModuloVisible(cursoId, moduloId, actorId(principal));
            redirect.addFlashAttribute("panelOk", "La visibilidad del módulo se actualizó.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
        }
        return "redirect:" + estructuraUrl(cursoId) + "#modulo-" + moduloId;
    }

    @PostMapping("/secciones/{seccionId}/visible")
    public String toggleSeccionVisible(@PathVariable UUID cursoId, @PathVariable UUID seccionId,
                                       @AuthenticationPrincipal AutenticadoUsuario principal,
                                       RedirectAttributes redirect) {
        try {
            adminContenido.toggleSeccionVisible(cursoId, seccionId, actorId(principal));
            redirect.addFlashAttribute("panelOk", "La visibilidad de la sección se actualizó.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
        }
        return "redirect:" + estructuraUrl(cursoId) + "#temario";
    }

    @PostMapping("/items/{itemId}/visible")
    public String toggleItemVisible(@PathVariable UUID cursoId, @PathVariable UUID itemId,
                                    @AuthenticationPrincipal AutenticadoUsuario principal,
                                    RedirectAttributes redirect) {
        try {
            adminContenido.toggleItemVisible(cursoId, itemId, actorId(principal));
            redirect.addFlashAttribute("panelOk", "La visibilidad del ítem se actualizó.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
        }
        return "redirect:" + estructuraUrl(cursoId) + "#item-" + itemId;
    }

    @GetMapping("/items/{itemId}/eliminar")
    public String confirmDeleteItem(@PathVariable UUID cursoId, @PathVariable UUID itemId,
                                    Model model) {
        String titulo = buscarItemTitulo(cursoId, itemId);
        String base = estructuraUrl(cursoId);
        return confirmation(model, "cursos", "Eliminar ítem",
                "Se borrará el contenido y su avance asociado. Esta acción no se puede deshacer.",
                base + "/items/" + itemId + "/eliminar", "Eliminar ítem",
                base, List.of("Ítem: " + titulo));
    }

    @PostMapping("/items/{itemId}/eliminar")
    public String deleteItem(@PathVariable UUID cursoId, @PathVariable UUID itemId,
                             @AuthenticationPrincipal AutenticadoUsuario principal,
                             RedirectAttributes redirect) {
        try {
            adminContenido.deleteItem(itemId, actorId(principal));
            redirect.addFlashAttribute("panelOk", "El ítem se eliminó.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
        }
        return "redirect:" + estructuraUrl(cursoId);
    }

    @PostMapping("/syllabus")
    public String saveSilabo(@PathVariable UUID cursoId,
                               @Valid @ModelAttribute AdminSilaboFormulario formulario,
                               BindingResult resultado,
                               @AuthenticationPrincipal AutenticadoUsuario principal,
                               Model model,
                               RedirectAttributes redirect) {
        if (resultado.hasErrors()) return invalido(cursoId, model);
        try {
            List<AdminCursoContenidoService.RecursoFormulario> resources = new ArrayList<>();
            List<CursoContenidoService.RecursoVista> actual = content.silabo(cursoId).resources();
            for (int i = 0; i < actual.size(); i++) {
                CursoContenidoService.RecursoVista recurso = actual.get(i);
                resources.add(new AdminCursoContenidoService.RecursoFormulario(recurso.id(),
                        recurso.titulo(), recurso.url(), i));
            }
            adminContenido.saveSilabo(cursoId,
                    new AdminCursoContenidoService.SilaboFormulario(formulario.objectives(),
                            formulario.requirements(), resources), actorId(principal));
            redirect.addFlashAttribute("panelOk", "El sílabo se guardó.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
        }
        return "redirect:" + estructuraUrl(cursoId) + "#syllabus";
    }

    @PostMapping("/recursos")
    public String createRecurso(@PathVariable UUID cursoId,
                                 @Valid @ModelAttribute AdminRecursoFormulario formulario,
                                 BindingResult resultado,
                                 @AuthenticationPrincipal AutenticadoUsuario principal,
                                 Model model,
                                 RedirectAttributes redirect) {
        if (resultado.hasErrors()) return invalido(cursoId, model);
        try {
            adminContenido.createRecurso(cursoId, new AdminCursoContenidoService.RecursoFormulario(null,
                    formulario.titulo(), formulario.url(), formulario.posicion()), actorId(principal));
            redirect.addFlashAttribute("panelOk", "El recurso se agregó.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
        }
        return "redirect:" + estructuraUrl(cursoId) + "#syllabus";
    }

    @PostMapping("/recursos/{recursoId}/eliminar")
    public String deleteRecurso(@PathVariable UUID cursoId, @PathVariable UUID recursoId,
                                 @AuthenticationPrincipal AutenticadoUsuario principal,
                                 RedirectAttributes redirect) {
        try {
            adminContenido.deleteRecurso(recursoId, actorId(principal));
            redirect.addFlashAttribute("panelOk", "El recurso se quitó.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("panelError", exception.getMessage());
        }
        return "redirect:" + estructuraUrl(cursoId) + "#syllabus";
    }

    private String render(UUID cursoId, Model model) {
        forms(model, load(cursoId, model));
        return "admin/contenido";
    }

    /** Datos de solo lectura: se usan tanto al abrir como al volver por una validación. */
    private CursoContenidoService.Silabo load(UUID cursoId, Model model) {
        AdminCursoFormulario curso = catalog.cursoFormulario(cursoId);
        CursoContenidoService.Silabo silabo = content.silabo(cursoId);
        CursoContenidoService.Esquema esquema = content.esquema(cursoId, null, null, true);
        common(model, "cursos", "Contenido de " + curso.titulo());
        model.addAttribute("cursoId", cursoId);
        model.addAttribute("cursoTitulo", curso.titulo());
        model.addAttribute("cursoSlug", curso.slug());
        model.addAttribute("esquema", esquema);
        model.addAttribute("silabo", silabo);
        List<AdminOpcion> modules = new ArrayList<>();
        List<AdminOpcion> sections = new ArrayList<>();
        sections.add(new AdminOpcion(null, "Sin sección"));
        int moduloNumber = 0;
        for (CursoContenidoService.ModuloVista modulo : esquema.modules()) {
            moduloNumber++;
            String label = "Módulo " + moduloNumber + " — " + modulo.titulo();
            modules.add(new AdminOpcion(modulo.id(), label));
            for (CursoContenidoService.SeccionVista seccion : modulo.sections()) {
                sections.add(new AdminOpcion(seccion.id(), label + " / " + seccion.titulo()));
            }
        }
        model.addAttribute("moduloOptions", modules);
        model.addAttribute("seccionOptions", sections);
        return silabo;
    }

    /** Mantiene los formularios enlazados por Spring cuando hay errores de validación. */
    private void forms(Model model, CursoContenidoService.Silabo silabo) {
        addIfMissing(model, "seccionFormulario", "adminSeccionFormulario",
                new AdminSeccionFormulario(null, "", 0, true));
        addIfMissing(model, "itemFormulario", "adminItemFormulario",
                new AdminItemFormulario(null, null, "MATERIAL", "", "", "", null, null, 0, true));
        addIfMissing(model, "documentoFormulario", "adminDocumentoFormulario",
                new AdminDocumentFormulario(null, null, "", "", null, 0, true));
        addIfMissing(model, "silaboFormulario", "adminSilaboFormulario",
                new AdminSilaboFormulario(silabo.objectives(), silabo.requirements()));
        addIfMissing(model, "recursoFormulario", "adminRecursoFormulario",
                new AdminRecursoFormulario("", "", 0));
    }

    private void addIfMissing(Model model, String alias, String boundNombre, Object formulario) {
        if (!model.containsAttribute(boundNombre)) model.addAttribute(boundNombre, formulario);
        if (!model.containsAttribute(alias)) model.addAttribute(alias, model.asMap().get(boundNombre));
    }

    private String invalido(UUID cursoId, Model model) {
        model.addAttribute("panelError", "Revisá los campos marcados.");
        forms(model, load(cursoId, model));
        return "admin/contenido";
    }

    private String estructuraUrl(UUID cursoId) {
        return "/admin/cursos/" + cursoId + "/contenido";
    }

    private String buscarSeccionTitulo(UUID cursoId, UUID seccionId) {
        return content.esquema(cursoId, null, null, true).modules().stream()
                .flatMap(modulo -> modulo.sections().stream())
                .filter(seccion -> seccion.id().equals(seccionId))
                .map(CursoContenidoService.SeccionVista::titulo)
                .findFirst().orElse("Sección");
    }

    private String buscarItemTitulo(UUID cursoId, UUID itemId) {
        CursoContenidoService.Esquema esquema = content.esquema(cursoId, null, null, true);
        return esquema.modules().stream()
                .flatMap(modulo -> java.util.stream.Stream.concat(
                        modulo.sections().stream().flatMap(seccion -> seccion.items().stream()),
                        modulo.items().stream()))
                .filter(item -> item.id().equals(itemId))
                .map(CursoContenidoService.ItemVista::titulo)
                .findFirst().orElse("Ítem");
    }
}
