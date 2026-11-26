package com.mundomanitas.api.repository;

import com.mundomanitas.api.model.CuestionarioIntentoRespuesta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface CuestionarioIntentoRespuestaRepository extends JpaRepository<CuestionarioIntentoRespuesta, UUID> {

    @Query("""
            select x from CuestionarioIntentoRespuesta x where x.intentoId = ?1
    """)
    List<CuestionarioIntentoRespuesta> buscarPorIntentoId(UUID intentoId);
}
