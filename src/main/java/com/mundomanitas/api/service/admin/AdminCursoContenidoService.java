package com.mundomanitas.api.service.admin;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/** Alta, edición y orden de la estructura de contenido del curso. */
public interface AdminCursoContenidoService {

    record SeccionFormulario(String titulo, int posicion, boolean visible) {
    }

    record ItemFormulario(UUID moduloId, UUID seccionId, String type, String titulo, String descripcion,
                    String url, String youtubeUrl, Integer duracionSegundos, int posicion,
                    boolean visible) {
    }

    record DocumentFormulario(UUID moduloId, UUID seccionId, String titulo, String descripcion,
                        MultipartFile file, Integer duracionSegundos, int posicion, boolean visible) {
    }

    record RecursoFormulario(UUID id, String titulo, String url, int posicion) {
    }

    record SilaboFormulario(String objectives, String requirements, List<RecursoFormulario> resources) {
    }

    UUID createSeccion(UUID moduloId, SeccionFormulario formulario, UUID actor);

    void updateSeccion(UUID seccionId, SeccionFormulario formulario, UUID actor);

    void deleteSeccion(UUID seccionId, UUID actor);

    UUID createItem(ItemFormulario formulario, UUID actor);

    UUID createDocument(DocumentFormulario formulario, UUID actor);

    void updateItem(UUID itemId, ItemFormulario formulario, UUID actor);

    void deleteItem(UUID itemId, UUID actor);

    void moveSeccion(UUID seccionId, boolean up, UUID actor);

    void moveItem(UUID itemId, boolean up, UUID actor);

    void toggleModuloVisible(UUID cursoId, UUID moduloId, UUID actor);

    void toggleSeccionVisible(UUID cursoId, UUID seccionId, UUID actor);

    void toggleItemVisible(UUID cursoId, UUID itemId, UUID actor);

    void saveSilabo(UUID cursoId, SilaboFormulario formulario, UUID actor);

    UUID createRecurso(UUID cursoId, RecursoFormulario formulario, UUID actor);

    void deleteRecurso(UUID recursoId, UUID actor);
}
