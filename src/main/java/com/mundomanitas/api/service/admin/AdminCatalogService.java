package com.mundomanitas.api.service.admin;

import com.mundomanitas.api.dto.admin.AdminCursoFormulario;
import com.mundomanitas.api.dto.admin.AdminFilaCurso;
import com.mundomanitas.api.dto.admin.AdminEjercicioFormulario;
import com.mundomanitas.api.dto.admin.AdminModuloFormulario;
import com.mundomanitas.api.dto.admin.AdminOpcion;
import com.mundomanitas.api.dto.admin.AdminUsuarioFormulario;
import com.mundomanitas.api.dto.admin.AdminFilaUsuario;
import com.mundomanitas.api.dto.admin.AdminVideoFormulario;
import com.mundomanitas.api.dto.admin.FilaEjercicio;
import com.mundomanitas.api.dto.admin.FilaModulo;
import com.mundomanitas.api.dto.admin.FilaVideo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** CRUD administrativo de cuentas, cursos y contenido, con auditoría. */
public interface AdminCatalogService {

    /** Mejoras pendientes de un curso ya visible; nunca bloquean la publicación. */
    record PublicarLista(List<String> missing, boolean canPublicar) {
    }

    /** Impacto real de borrar un curso, calculado desde la base. */
    record EliminacionImpacto(int modules, int videos, int exercises,
                          long enrollments, long certificates) {
    }

    String slugFromTitulo(String titulo);

    Page<AdminFilaUsuario> users(String rol, String term, Pageable pageable, UUID actualId);

    AdminUsuarioFormulario usuarioFormulario(UUID id);

    void updateUsuario(UUID id, AdminUsuarioFormulario formulario, UUID actor);

    Page<AdminFilaCurso> courses(String term, Pageable pageable);

    AdminCursoFormulario cursoFormulario(UUID id);

    UUID createCurso(AdminCursoFormulario formulario, UUID actor);

    void updateCurso(UUID id, AdminCursoFormulario formulario, UUID actor);

    /** Guarda la portada local y devuelve la ruta anterior para limpiar el archivo. */
    String updateCursoPortada(UUID id, String imagen, UUID actor);

    PublicarLista publicarLista(UUID id);

    String cursoEstado(UUID id);

    void setPublished(UUID id, boolean published, UUID actor);

    EliminacionImpacto eliminacionImpacto(UUID id);

    void deleteCurso(UUID id, UUID actor);

    int leccionContar(UUID cursoId);

    void moveModulo(UUID id, boolean up, UUID actor);

    void moveVideo(UUID id, boolean up, UUID actor);

    Page<FilaModulo> modules(UUID cursoId, String term, Pageable pageable);

    AdminModuloFormulario moduloFormulario(UUID id);

    UUID createModulo(AdminModuloFormulario formulario, UUID actor);

    void updateModulo(UUID id, AdminModuloFormulario formulario, UUID actor);

    void deleteModulo(UUID id, UUID actor);

    Page<FilaVideo> videos(UUID moduloId, String term, Pageable pageable);

    /** Listado de videos acotado primero por curso y después por módulo. */
    Page<FilaVideo> videos(UUID cursoId, UUID moduloId, String term, Pageable pageable);

    AdminVideoFormulario videoFormulario(UUID id);

    UUID createVideo(AdminVideoFormulario formulario, UUID actor);

    void updateVideo(UUID id, AdminVideoFormulario formulario, UUID actor);

    void deleteVideo(UUID id, UUID actor);

    Page<FilaEjercicio> exercises(UUID moduloId, String term, Pageable pageable);

    /** Listado de ejercicios acotado primero por curso y después por módulo. */
    Page<FilaEjercicio> exercises(UUID cursoId, UUID moduloId, String term, Pageable pageable);

    /** Cantidad de preguntas cargadas, para mostrar el estado del cuestionario. */
    long contarPreguntas(UUID ejercicioId);

    AdminEjercicioFormulario ejercicioFormulario(UUID id);

    UUID createEjercicio(AdminEjercicioFormulario formulario, UUID actor);

    void updateEjercicio(UUID id, AdminEjercicioFormulario formulario, UUID actor);

    void deleteEjercicio(UUID id, UUID actor);

    List<AdminOpcion> cursoOptions();

    List<AdminOpcion> moduloOptions();

    Map<UUID, String> usuarioNames();
}
