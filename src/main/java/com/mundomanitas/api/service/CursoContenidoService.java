package com.mundomanitas.api.service;

import com.mundomanitas.api.model.Usuario;

import java.util.List;
import java.util.UUID;

/**
 * Estructura y avance reales del contenido de un curso.
 *
 * <p>El estado de un VIDEO se lee del progreso existente por video; el de un
 * MATERIAL, del avance por ítem; y el de un EJERCICIO, del cuestionario aprobado
 * con 20/20. Los porcentajes cuentan solo los ítems visibles que existen en
 * PostgreSQL.</p>
 */
public interface CursoContenidoService {

    record RecursoVista(UUID id, String titulo, String url) {
    }

    record Silabo(String resumen, String objectives, String requirements,
                    List<RecursoVista> resources) {
    }

    record ItemVista(UUID id, String type, String titulo, String descripcion, String url,
                    String youtubeId, Integer duracionSegundos, boolean visible, boolean visto,
                    boolean bloqueado, UUID origenId, String nombreArchivo, boolean cuestionarioDone) {

        /** Conserva la firma anterior para vistas que no necesitan datos del archivo. */
        public ItemVista(UUID id, String type, String titulo, String descripcion, String url,
                        String youtubeId, Integer duracionSegundos, boolean visible, boolean visto,
                        boolean bloqueado) {
            this(id, type, titulo, descripcion, url, youtubeId, duracionSegundos, visible, visto,
                    bloqueado, null, null, false);
        }

        /** Conserva la firma con video para vistas sin documento subido. */
        public ItemVista(UUID id, String type, String titulo, String descripcion, String url,
                        String youtubeId, Integer duracionSegundos, boolean visible, boolean visto,
                        boolean bloqueado, UUID origenId) {
            this(id, type, titulo, descripcion, url, youtubeId, duracionSegundos, visible, visto,
                    bloqueado, origenId, null, false);
        }

        /** Conserva la firma sin dato de cuestionario para vistas administrativas. */
        public ItemVista(UUID id, String type, String titulo, String descripcion, String url,
                        String youtubeId, Integer duracionSegundos, boolean visible, boolean visto,
                        boolean bloqueado, UUID origenId, String nombreArchivo) {
            this(id, type, titulo, descripcion, url, youtubeId, duracionSegundos, visible, visto,
                    bloqueado, origenId, nombreArchivo, false);
        }

        /** true cuando el material se descarga desde el panel, no desde un enlace externo. */
        public boolean downloadable() {
            return url != null && url.startsWith(CursoMaterialStorage.URL_PREFIX);
        }

        /** Duración legible en el temario: segundos, minutos u horas según corresponda. */
        public String duracionLabel() {
            if (duracionSegundos == null) {
                return "Sin duración";
            }
            int total = Math.max(0, duracionSegundos);
            int hours = total / 3600;
            int minutes = (total % 3600) / 60;
            int segundos = total % 60;
            if (hours > 0) {
                return minutes > 0 ? hours + " h " + minutes + " min" : hours + " h";
            }
            if (minutes > 0) {
                return segundos > 0 ? minutes + " min " + segundos + " s" : minutes + " min";
            }
            return segundos + " s";
        }
    }

    record SeccionVista(UUID id, String titulo, boolean visible, List<ItemVista> items) {
    }

    record ModuloVista(UUID id, String titulo, boolean visible, List<SeccionVista> sections,
                      List<ItemVista> items, int percent) {
    }

    record Esquema(List<ModuloVista> modules, int totalItems, int doneItems, int percent,
                   UUID nextPending) {
    }

    record Progreso(int done, int total, int percent) {
    }

    record MaterialDescarga(String nombreArchivo, String contenidoTipo, byte[] bytes) {

        /** Nombre seguro para la cabecera de descarga. */
        public String safeFileNombre() {
            return nombreArchivo.replace('\r', '_').replace('\n', '_').replace('"', '_');
        }
    }

    /** Datos del PDF que se abre en el escenario del curso. */
    record MaterialVista(UUID id, String titulo, String nombreArchivo, String contenidoTipo) {
    }

    /** Clase a reproducir: video normal o versión en lengua de señas del índice. */
    record VideoItemVista(UUID itemId, UUID videoId, String youtubeId, String titulo, boolean lsp) {
    }

    Silabo silabo(UUID cursoId);

    /** Temario con secciones y estados; {@code includeHidden} habilita la vista previa. */
    Esquema esquema(UUID cursoId, UUID inscripcionId, Usuario viewer, boolean includeHidden);

    Progreso progreso(UUID inscripcionId, UUID cursoId);

    UUID nextPending(UUID cursoId, UUID inscripcionId);

    /** Marca o desmarca un ítem para la matrícula de la persona. */
    void markVisto(Usuario usuario, String slug, UUID itemId, boolean visto);

    /** Documento subido, solo para una matrícula válida y con plan habilitado. */
    MaterialDescarga descargaMaterial(Usuario usuario, String slug, UUID itemId);

    /** Ficha del PDF a abrir, sin leer el archivo, con la matrícula ya validada. */
    MaterialVista vistaMaterial(Usuario usuario, String slug, UUID itemId);

    /** Resuelve un índice de video según el identificador LSP de la cuenta. */
    VideoItemVista videoItem(Usuario usuario, String slug, UUID itemId);

    /** Bytes del PDF para el visor, con la misma validación que la descarga. */
    MaterialDescarga leidoPdf(Usuario usuario, String slug, UUID itemId);
}
