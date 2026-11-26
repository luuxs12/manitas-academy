package com.mundomanitas.api.service;

import com.mundomanitas.api.model.Inscripcion;
import com.mundomanitas.api.model.LeccionProgreso;
import com.mundomanitas.api.model.Usuario;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Avance real por video dentro de una matrícula.
 */
public interface LeccionProgresoService {

    List<LeccionProgreso> forInscripcion(UUID inscripcionId);

    int finalizado(UUID inscripcionId);

    Set<UUID> finalizadoVideos(UUID inscripcionId);

    /** Videos vistos de varias matrículas, agrupados por matrícula. */
    Map<UUID, Set<UUID>> finalizadoVideosFor(Set<UUID> inscripcionIds);

    boolean toggle(Usuario usuario, String slug, UUID videoId);

    void completeAll(Inscripcion inscripcion);

    LeccionTotals totals(List<Inscripcion> mine);

    record LeccionTotals(int finalizado, int total, int percent) {
    }
}
