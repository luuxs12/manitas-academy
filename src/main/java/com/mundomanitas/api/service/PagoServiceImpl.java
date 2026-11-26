package com.mundomanitas.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.mundomanitas.api.actuator.BusinessMetrics;
import com.mundomanitas.api.config.StripeProperties;
import com.mundomanitas.api.exception.PagoNoDisponibleException;
import com.mundomanitas.api.model.Pago;
import com.mundomanitas.api.model.Plan;
import com.mundomanitas.api.model.Usuario;
import com.mundomanitas.api.repository.PagoRepository;
import com.mundomanitas.api.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Implementación de los cobros de la membresía. */
@Service
public class PagoServiceImpl implements PagoService {

    private static final Logger log = LoggerFactory.getLogger(PagoServiceImpl.class);
    private static final Set<String> PAID_STATUSES = Set.of("paid", "no_payment_required");

    private final StripeProperties properties;
    private final StripeClient stripe;
    private final PagoRepository payments;
    private final UsuarioRepository users;
    private final NotificacionService notifications;
    private final BusinessMetrics metrics;

    public PagoServiceImpl(
            StripeProperties properties,
            StripeClient stripe,
            PagoRepository payments,
            UsuarioRepository users,
            NotificacionService notifications,
            BusinessMetrics metrics
    ) {
        this.properties = properties;
        this.stripe = stripe;
        this.payments = payments;
        this.users = users;
        this.notifications = notifications;
        this.metrics = metrics;
    }

    @Override
    public boolean isEnabled() {
        return properties.isEnabled();
    }

    @Override
    public boolean isEmbeddedEnabled() {
        return properties.isEnabled() && !properties.getPublishableClave().isBlank();
    }

    @Override
    public String publishableClave() {
        return properties.getPublishableClave();
    }

    @Override
    public String customerPortalUrl() {
        return properties.getCustomerPortalUrl();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pago> history(Usuario usuario) {
        return payments.buscarPorUsuarioOrdenarPorFechaCreacionDesc(usuario);
    }

    /** Inicia un checkout real; el plan se activa recién en el webhook. */
    @Override
    @Transactional
    public String checkout(Usuario usuario, Plan plan, String successUrl, String cancelUrl) {
        if (!plan.isPremium()) {
            throw new PagoNoDisponibleException("Ese plan no requiere pago.");
        }
        if (!properties.isEnabled()) {
            throw new PagoNoDisponibleException(
                    "El cobro todavía no está habilitado. Volvé a intentarlo cuando esté disponible.");
        }

        StripeClient.Sesion session;
        try {
            session = stripe.createCheckoutSesion(usuario, plan, successUrl, cancelUrl);
        } catch (RuntimeException exception) {
            log.warn("No se pudo iniciar el checkout para usuario={}", usuario.getId());
            throw new PagoNoDisponibleException(
                    "No se pudo iniciar el pago. Intentá de nuevo en unos minutos.");
        }

        Pago pago = new Pago(usuario, plan);
        pago.enlaceSesion(session.id());
        payments.save(pago);
        metrics.checkoutIniciado(plan.name());
        log.info("Checkout iniciado usuario={} plan={} sesión={}",
                usuario.getId(), plan, session.id());
        return session.url();
    }

    /**
     * Inicia un Checkout embebido. El plan se activa con el webhook o al volver
     * con el session_id; acá solo se crea el movimiento pendiente.
     */
    @Override
    @Transactional
    public EmbeddedCheckout embeddedCheckout(Usuario usuario, Plan plan, String returnUrl) {
        if (!plan.isPremium()) {
            throw new PagoNoDisponibleException("Ese plan no requiere pago.");
        }
        if (!isEmbeddedEnabled()) {
            throw new PagoNoDisponibleException(
                    "El pago embebido todavía no está habilitado en este entorno.");
        }

        StripeClient.EmbeddedSesion session;
        try {
            session = stripe.createEmbeddedCheckoutSesion(usuario, plan, returnUrl);
        } catch (RuntimeException exception) {
            log.warn("No se pudo iniciar el checkout embebido para usuario={}", usuario.getId());
            throw new PagoNoDisponibleException(
                    "No se pudo iniciar el pago. Intentá de nuevo en unos minutos.");
        }

        Pago pago = new Pago(usuario, plan);
        pago.enlaceSesion(session.id());
        payments.save(pago);
        metrics.checkoutIniciado(plan.name());
        log.info("Checkout embebido iniciado usuario={} plan={} sesión={}",
                usuario.getId(), plan, session.id());
        return new EmbeddedCheckout(session.clientSecret(), session.id());
    }

    /** Confirma el pago al volver del checkout, por si el webhook se demoró. */
    @Override
    @Transactional
    public void confirmDespuesReturn(Usuario usuario, String sesionId) {
        if (!properties.isEnabled() || sesionId == null || sesionId.isBlank()) return;
        JsonNode session = stripe.retrieveSesion(sesionId);
        if (!PAID_STATUSES.contains(session.path("payment_status").asText())) return;
        if (!belongsTo(usuario, session)) return;
        activate(session);
    }

    /** Procesa un evento verificado de Stripe. */
    @Override
    @Transactional
    public void handleEvento(JsonNode evento) {
        String type = evento.path("type").asText();
        JsonNode object = evento.path("data").path("object");

        switch (type) {
            case "checkout.session.completed" -> {
                if (PAID_STATUSES.contains(object.path("payment_status").asText())) {
                    activate(object);
                }
            }
            case "checkout.session.expired" -> payments.buscarPorStripeSesionId(object.path("id").asText())
                    .ifPresent(pago -> {
                        pago.markFailed();
                        payments.save(pago);
                        metrics.pagoFailed();
                        log.info("Checkout vencido marcado como fallido sesión={}",
                                pago.getStripeSesionId());
                    });
            case "customer.subscription.updated" -> subscriptionActualizado(object);
            case "customer.subscription.deleted" -> subscriptionEliminado(object);
            case "charge.refunded" -> refunded(object);
            default -> { /* eventos que no cambian el plan */ }
        }
    }

    @Override
    @Transactional
    public void cancelAtPeriodFin(Usuario usuario) {
        Optional<Pago> last = payments.buscarPorUsuarioOrdenarPorFechaCreacionDesc(usuario).stream()
                .filter(pago -> pago.getStripeSubscriptionId() != null)
                .findFirst();
        if (last.isEmpty()) {
            throw new PagoNoDisponibleException("No hay una suscripción activa para cancelar.");
        }
        if (!properties.isEnabled()) {
            throw new PagoNoDisponibleException("La cancelación no está disponible en este momento.");
        }
        stripe.cancelSubscriptionAtPeriodFin(last.get().getStripeSubscriptionId());
        usuario.cancelSubscriptionAtPeriodFin();
        users.save(usuario);
        notifications.notify(
                usuario,
                NotificacionService.PAYMENT,
                "Suscripción en cancelación",
                "Vas a conservar el acceso premium hasta el final del período pagado.",
                "/panel/facturacion"
        );
        log.info("Suscripción marcada para cancelar usuario={}", usuario.getId());
    }

    private void activate(JsonNode session) {
        String sesionId = session.path("id").asText();
        Pago pago = payments.buscarPorStripeSesionId(sesionId).orElseGet(() -> {
            UUID usuarioId = parseUuid(session.path("metadata").path("user_id").asText());
            Plan plan = parsePlan(session.path("metadata").path("plan").asText());
            Usuario owner = usuarioId == null
                    ? null
                    : users.findById(usuarioId).orElse(null);
            if (owner == null || plan == null) {
                throw new PagoNoDisponibleException("El pago no corresponde a una cuenta.");
            }
            Pago creado = new Pago(
                    owner, plan, session.path("amount_total").asInt());
            creado.enlaceSesion(sesionId);
            return payments.save(creado);
        });

        if (pago.isPagado()) return;

        String subscriptionId = text(session.path("subscription"));
        String pagoIntentId = text(session.path("payment_intent"));
        OffsetDateTime periodFin = subscriptionId.isBlank()
                ? null
                : periodFin(stripe.retrieveSubscription(subscriptionId));

        pago.markPagado(pagoIntentId, subscriptionId.isBlank() ? null : subscriptionId, periodFin);
        payments.save(pago);

        Usuario owner = pago.getUsuario();
        owner.activateSubscription(pago.getPlan(), periodFin);
        if (!subscriptionId.isBlank()) {
            owner.changeStripeCustomerId(text(session.path("customer")));
        }
        users.save(owner);
        metrics.pagoConfirmed(pago.getPlan().name());
        notifications.notify(
                owner,
                NotificacionService.PAYMENT,
                "Pago confirmado",
                "Tu plan " + pago.getPlan().getLabel() + " ya está activo.",
                "/panel/facturacion"
        );
        log.info("Pago confirmado usuario={} plan={} sesión={}",
                owner.getId(), pago.getPlan(), sesionId);
    }

    private void subscriptionActualizado(JsonNode subscription) {
        String subscriptionId = text(subscription.path("id"));
        Pago pago = payments.buscarPorStripeSubscriptionId(subscriptionId).orElse(null);
        if (pago == null) return;

        Usuario owner = pago.getUsuario();
        String status = text(subscription.path("status"));
        if ("canceled".equals(status) || "unpaid".equals(status)) {
            owner.expiraSubscription();
            notifications.notify(
                    owner,
                    NotificacionService.PAYMENT,
                    "Acceso premium finalizado",
                    "Tu cuenta volvió al plan gratis; tu progreso se conserva.",
                    "/panel/perfil#plan"
            );
        } else {
            owner.updateSubscriptionFin(periodFin(subscription));
            if (subscription.path("cancel_at_period_end").asBoolean(false)) {
                owner.cancelSubscriptionAtPeriodFin();
            } else {
                owner.activateSubscription(pago.getPlan(), periodFin(subscription));
            }
        }
        users.save(owner);
        log.info("Suscripción actualizada usuario={} estado={}", owner.getId(), status);
    }

    private void subscriptionEliminado(JsonNode subscription) {
        payments.buscarPorStripeSubscriptionId(text(subscription.path("id")))
                .ifPresent(pago -> {
                    Usuario owner = pago.getUsuario();
                    owner.expiraSubscription();
                    users.save(owner);
                    notifications.notify(
                            owner,
                            NotificacionService.PAYMENT,
                            "Suscripción cancelada",
                            "Tu cuenta volvió al plan gratis; tu progreso se conserva.",
                            "/panel/perfil#plan"
                    );
                    log.info("Suscripción eliminada usuario={}", owner.getId());
                });
    }

    private void refunded(JsonNode charge) {
        payments.buscarPorStripePagoIntentId(text(charge.path("payment_intent")))
                .ifPresent(pago -> {
                    pago.markRefunded();
                    payments.save(pago);
                    Usuario owner = pago.getUsuario();
                    owner.expiraSubscription();
                    users.save(owner);
                    notifications.notify(
                            owner,
                            NotificacionService.PAYMENT,
                            "Pago reembolsado",
                            "La suscripción asociada a ese pago fue cancelada.",
                            "/panel/facturacion"
                    );
                    log.info("Pago reembolsado usuario={}", owner.getId());
                });
    }

    private boolean belongsTo(Usuario usuario, JsonNode session) {
        String id = text(session.path("metadata").path("user_id"));
        return usuario.getId().toString().equals(id)
                || payments.buscarPorStripeSesionId(text(session.path("id")))
                        .map(pago -> pago.getUsuario().getId().equals(usuario.getId()))
                        .orElse(false);
    }

    private OffsetDateTime periodFin(JsonNode subscription) {
        long segundos = subscription.path("current_period_end").asLong(0);
        return segundos == 0 ? null : OffsetDateTime.ofInstant(Instant.ofEpochSecond(segundos), ZoneOffset.UTC);
    }

    private String text(JsonNode node) {
        return node.isMissingNode() || node.isNull() ? "" : node.asText();
    }

    private UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private Plan parsePlan(String value) {
        try {
            Plan plan = Plan.valueOf(value);
            return plan.isPremium() ? plan : null;
        } catch (RuntimeException exception) {
            return null;
        }
    }
}
