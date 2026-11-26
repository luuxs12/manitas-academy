package com.mundomanitas.api.repository;

import com.mundomanitas.api.model.Pago;
import com.mundomanitas.api.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PagoRepository extends JpaRepository<Pago, UUID> {

    @Query("""
            select x from Pago x where x.usuario = ?1 order by x.fechaCreacion desc
    """)
    List<Pago> buscarPorUsuarioOrdenarPorFechaCreacionDesc(Usuario usuario);

    @Query("""
            select x from Pago x where x.stripeSesionId = ?1
    """)
    Optional<Pago> buscarPorStripeSesionId(String sesionId);

    @Query("""
            select x from Pago x where x.stripePagoIntentId = ?1
    """)
    Optional<Pago> buscarPorStripePagoIntentId(String pagoIntentId);

    @Query("""
            select x from Pago x where x.stripeSubscriptionId = ?1
    """)
    Optional<Pago> buscarPorStripeSubscriptionId(String subscriptionId);

    @Query("select coalesce(sum(p.amountCents), 0) from Pago p where p.status = 'PAID'")
    long pagadoTotalCents();

    @Query("""
            select count(x) from Pago x where x.status = ?1 and x.fechaPago between ?2 and ?3
    """)
    long contarPorEstadoAndFechaPagoEntre(String status, OffsetDateTime from, OffsetDateTime to);

    @Query("""
            select x from Pago x where x.status = ?1 order by x.fechaPago desc limit 5
    """)
    List<Pago> buscarTop5PorEstadoOrdenarPorFechaPagoDesc(String status);
}
