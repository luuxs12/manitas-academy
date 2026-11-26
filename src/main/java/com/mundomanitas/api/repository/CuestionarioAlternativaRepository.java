package com.mundomanitas.api.repository;

import com.mundomanitas.api.model.CuestionarioAlternativa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CuestionarioAlternativaRepository extends JpaRepository<CuestionarioAlternativa, UUID> {

    @Query("""
            select x from CuestionarioAlternativa x where x.preguntaId in ?1 order by x.preguntaId asc, x.posicion asc
    """)
    List<CuestionarioAlternativa> buscarPorPreguntaIdInOrdenarPorPreguntaIdAscPosicionAsc( Collection<UUID> preguntaIds);
}
