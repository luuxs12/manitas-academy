package com.mundomanitas.api.repository;

import com.mundomanitas.api.model.ContrasenaRestablecerToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/** Consultas de los enlaces de restablecimiento. */
public interface ContrasenaRestablecerTokenRepository extends JpaRepository<ContrasenaRestablecerToken, UUID> {

    @Query("""
            select x from ContrasenaRestablecerToken x where x.tokenHash = ?1
    """)
    Optional<ContrasenaRestablecerToken> buscarPorTokenHash(String tokenHash);

    @Modifying
    @Query("delete from ContrasenaRestablecerToken token where token.usuario.id = :usuarioId")
    void deleteAllForUsuario(@Param("usuarioId") UUID usuarioId);
}
