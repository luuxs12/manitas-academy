package com.mundomanitas.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.mundomanitas.api.actuator.BusinessMetrics;
import com.mundomanitas.api.config.StripeProperties;
import com.mundomanitas.api.exception.PaymentUnavailableException;
import com.mundomanitas.api.model.Payment;
import com.mundomanitas.api.model.Plan;
import com.mundomanitas.api.model.User;
import com.mundomanitas.api.repository.PaymentRepository;
import com.mundomanitas.api.repository.UserRepository;
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
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);
    private static final Set<String> PAID_STATUSES = Set.of("paid", "no_payment_required");

    private final StripeProperties properties;
    private final StripeClient stripe;
    private final PaymentRepository payments;
    private final UserRepository users;
    private final NotificationService notifications;
    private final BusinessMetrics metrics;

    public PaymentServiceImpl(
            StripeProperties properties,
            StripeClient stripe,
            PaymentRepository payments,
            UserRepository users,
            NotificationService notifications,
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
        return properties.isEnabled() && !properties.getPublishableKey().isBlank();
    }

    @Override
    public String publishableKey() {
        return properties.getPublishableKey();
    }

    @Override
    public String customerPortalUrl() {
        return properties.getCustomerPortalUrl();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> history(User user) {
        return payments.findByUserOrderByCreatedAtDesc(user);
    }

    /** Inicia un checkout real; el plan se activa recién en el webhook. */
    @Override
    @Transactional
    public String checkout(User user, Plan plan, String successUrl, String cancelUrl) {
        if (!plan.isPremium()) {
            throw new PaymentUnavailableException("Ese plan no requiere pago.");
        }
        if (!properties.isEnabled()) {
            throw new PaymentUnavailableException(
                    "El cobro todavía no está habilitado. Volvé a intentarlo cuando esté disponible.");
        }

        StripeClient.Session session;
        try {
            session = stripe.createCheckoutSession(user, plan, successUrl, cancelUrl);
        } catch (RuntimeException exception) {
            log.warn("No se pudo iniciar el checkout para usuario={}", user.getId());
            throw new PaymentUnavailableException(
                    "No se pudo iniciar el pago. Intentá de nuevo en unos minutos.");
        }

        Payment payment = new Payment(user, plan);
        payment.linkSession(session.id());
        payments.save(payment);
        metrics.checkoutStarted(plan.name());
        log.info("Checkout iniciado usuario={} plan={} sesión={}",
                user.getId(), plan, session.id());
        return session.url();
    }

    /**
     * Inicia un Checkout embebido. El plan se activa con el webhook o al volver
     * con el session_id; acá solo se crea el movimiento pendiente.
     */
    @Override
    @Transactional
    public EmbeddedCheckout embeddedCheckout(User user, Plan plan, String returnUrl) {
        if (!plan.isPremium()) {
            throw new PaymentUnavailableException("Ese plan no requiere pago.");
        }
        if (!isEmbeddedEnabled()) {
            throw new PaymentUnavailableException(
                    "El pago embebido todavía no está habilitado en este entorno.");
        }

        StripeClient.EmbeddedSession session;
        try {
            session = stripe.createEmbeddedCheckoutSession(user, plan, returnUrl);
        } catch (RuntimeException exception) {
            log.warn("No se pudo iniciar el checkout embebido para usuario={}", user.getId());
            throw new PaymentUnavailableException(
                    "No se pudo iniciar el pago. Intentá de nuevo en unos minutos.");
        }

        Payment payment = new Payment(user, plan);
        payment.linkSession(session.id());
        payments.save(payment);
        metrics.checkoutStarted(plan.name());
        log.info("Checkout embebido iniciado usuario={} plan={} sesión={}",
                user.getId(), plan, session.id());
        return new EmbeddedCheckout(session.clientSecret(), session.id());
    }

    /** Confirma el pago al volver del checkout, por si el webhook se demoró. */
    @Override
    @Transactional
    public void confirmAfterReturn(User user, String sessionId) {
        if (!properties.isEnabled() || sessionId == null || sessionId.isBlank()) return;
        JsonNode session = stripe.retrieveSession(sessionId);
        if (!PAID_STATUSES.contains(session.path("payment_status").asText())) return;
        if (!belongsTo(user, session)) return;
        activate(session);
    }

    /** Procesa un evento verificado de Stripe. */
    @Override
    @Transactional
    public void handleEvent(JsonNode event) {
        String type = event.path("type").asText();
        JsonNode object = event.path("data").path("object");

        switch (type) {
            case "checkout.session.completed" -> {
                if (PAID_STATUSES.contains(object.path("payment_status").asText())) {
                    activate(object);
                }
            }
            case "checkout.session.expired" -> payments.findByStripeSessionId(object.path("id").asText())
                    .ifPresent(payment -> {
                        payment.markFailed();
                        payments.save(payment);
                        metrics.paymentFailed();
                        log.info("Checkout vencido marcado como fallido sesión={}",
                                payment.getStripeSessionId());
                    });
            case "customer.subscription.updated" -> subscriptionUpdated(object);
            case "customer.subscription.deleted" -> subscriptionDeleted(object);
            case "charge.refunded" -> refunded(object);
            default -> { /* eventos que no cambian el plan */ }
        }
    }

    @Override
    @Transactional
    public void cancelAtPeriodEnd(User user) {
        Optional<Payment> last = payments.findByUserOrderByCreatedAtDesc(user).stream()
                .filter(payment -> payment.getStripeSubscriptionId() != null)
                .findFirst();
        if (last.isEmpty()) {
            throw new PaymentUnavailableException("No hay una suscripción activa para cancelar.");
        }
        if (!properties.isEnabled()) {
            throw new PaymentUnavailableException("La cancelación no está disponible en este momento.");
        }
        stripe.cancelSubscriptionAtPeriodEnd(last.get().getStripeSubscriptionId());
        user.cancelSubscriptionAtPeriodEnd();
        users.save(user);
        notifications.notify(
                user,
                NotificationService.PAYMENT,
                "Suscripción en cancelación",
                "Vas a conservar el acceso premium hasta el final del período pagado.",
                "/panel/facturacion"
        );
        log.info("Suscripción marcada para cancelar usuario={}", user.getId());
    }

    private void activate(JsonNode session) {
        String sessionId = session.path("id").asText();
        Payment payment = payments.findByStripeSessionId(sessionId).orElseGet(() -> {
            UUID userId = parseUuid(session.path("metadata").path("user_id").asText());
            Plan plan = parsePlan(session.path("metadata").path("plan").asText());
            User owner = userId == null
                    ? null
                    : users.findById(userId).orElse(null);
            if (owner == null || plan == null) {
                throw new PaymentUnavailableException("El pago no corresponde a una cuenta.");
            }
            Payment created = new Payment(
                    owner, plan, session.path("amount_total").asInt());
            created.linkSession(sessionId);
            return payments.save(created);
        });

        if (payment.isPaid()) return;

        String subscriptionId = text(session.path("subscription"));
        String paymentIntentId = text(session.path("payment_intent"));
        OffsetDateTime periodEnd = subscriptionId.isBlank()
                ? null
                : periodEnd(stripe.retrieveSubscription(subscriptionId));

        payment.markPaid(paymentIntentId, subscriptionId.isBlank() ? null : subscriptionId, periodEnd);
        payments.save(payment);

        User owner = payment.getUser();
        owner.activateSubscription(payment.getPlan(), periodEnd);
        if (!subscriptionId.isBlank()) {
            owner.changeStripeCustomerId(text(session.path("customer")));
        }
        users.save(owner);
        metrics.paymentConfirmed(payment.getPlan().name());
        notifications.notify(
                owner,
                NotificationService.PAYMENT,
                "Pago confirmado",
                "Tu plan " + payment.getPlan().getLabel() + " ya está activo.",
                "/panel/facturacion"
        );
        log.info("Pago confirmado usuario={} plan={} sesión={}",
                owner.getId(), payment.getPlan(), sessionId);
    }

    private void subscriptionUpdated(JsonNode subscription) {
        String subscriptionId = text(subscription.path("id"));
        Payment payment = payments.findByStripeSubscriptionId(subscriptionId).orElse(null);
        if (payment == null) return;

        User owner = payment.getUser();
        String status = text(subscription.path("status"));
        if ("canceled".equals(status) || "unpaid".equals(status)) {
            owner.expireSubscription();
            notifications.notify(
                    owner,
                    NotificationService.PAYMENT,
                    "Acceso premium finalizado",
                    "Tu cuenta volvió al plan gratis; tu progreso se conserva.",
                    "/panel/perfil#plan"
            );
        } else {
            owner.updateSubscriptionEnd(periodEnd(subscription));
            if (subscription.path("cancel_at_period_end").asBoolean(false)) {
                owner.cancelSubscriptionAtPeriodEnd();
            } else {
                owner.activateSubscription(payment.getPlan(), periodEnd(subscription));
            }
        }
        users.save(owner);
        log.info("Suscripción actualizada usuario={} estado={}", owner.getId(), status);
    }

    private void subscriptionDeleted(JsonNode subscription) {
        payments.findByStripeSubscriptionId(text(subscription.path("id")))
                .ifPresent(payment -> {
                    User owner = payment.getUser();
                    owner.expireSubscription();
                    users.save(owner);
                    notifications.notify(
                            owner,
                            NotificationService.PAYMENT,
                            "Suscripción cancelada",
                            "Tu cuenta volvió al plan gratis; tu progreso se conserva.",
                            "/panel/perfil#plan"
                    );
                    log.info("Suscripción eliminada usuario={}", owner.getId());
                });
    }

    private void refunded(JsonNode charge) {
        payments.findByStripePaymentIntentId(text(charge.path("payment_intent")))
                .ifPresent(payment -> {
                    payment.markRefunded();
                    payments.save(payment);
                    User owner = payment.getUser();
                    owner.expireSubscription();
                    users.save(owner);
                    notifications.notify(
                            owner,
                            NotificationService.PAYMENT,
                            "Pago reembolsado",
                            "La suscripción asociada a ese pago fue cancelada.",
                            "/panel/facturacion"
                    );
                    log.info("Pago reembolsado usuario={}", owner.getId());
                });
    }

    private boolean belongsTo(User user, JsonNode session) {
        String id = text(session.path("metadata").path("user_id"));
        return user.getId().toString().equals(id)
                || payments.findByStripeSessionId(text(session.path("id")))
                        .map(payment -> payment.getUser().getId().equals(user.getId()))
                        .orElse(false);
    }

    private OffsetDateTime periodEnd(JsonNode subscription) {
        long seconds = subscription.path("current_period_end").asLong(0);
        return seconds == 0 ? null : OffsetDateTime.ofInstant(Instant.ofEpochSecond(seconds), ZoneOffset.UTC);
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
