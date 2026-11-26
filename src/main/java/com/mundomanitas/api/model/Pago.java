package com.mundomanitas.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Cobro de una membresía, con los datos que informa Stripe. */
@Entity
@Table(name = "payments")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan", nullable = false)
    private Plan plan;

    @Column(name = "amount_cents", nullable = false)
    private int amountCents;

    @Column(name = "currency", nullable = false)
    private String currency = "PEN";

    @Column(name = "status", nullable = false)
    private String status = "PENDING";

    @Column(name = "stripe_session_id", unique = true)
    private String stripeSesionId;

    @Column(name = "stripe_payment_intent_id")
    private String stripePagoIntentId;

    @Column(name = "stripe_subscription_id")
    private String stripeSubscriptionId;

    @Column(name = "period_end")
    private OffsetDateTime periodFin;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "paid_at")
    private OffsetDateTime fechaPago;

    protected Pago() {
    }

    public Pago(Usuario usuario, Plan plan) {
        this.usuario = usuario;
        this.plan = plan;
        this.amountCents = plan.amountCents();
    }

    /** Monto informado por Stripe, que puede incluir impuestos del cobro real. */
    public Pago(Usuario usuario, Plan plan, int amountCents) {
        this(usuario, plan);
        if (amountCents > 0) {
            this.amountCents = amountCents;
        }
    }

    public UUID getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Plan getPlan() {
        return plan;
    }

    public int getAmountCents() {
        return amountCents;
    }

    public String getCurrency() {
        return currency;
    }

    public String getEstado() {
        return status;
    }

    public String getStripeSesionId() {
        return stripeSesionId;
    }

    public String getStripeSubscriptionId() {
        return stripeSubscriptionId;
    }

    public OffsetDateTime getPeriodFin() {
        return periodFin;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public OffsetDateTime getFechaPago() {
        return fechaPago;
    }

    public boolean isPagado() {
        return "PAID".equals(status);
    }

    public boolean isPending() {
        return "PENDING".equals(status);
    }

    public void enlaceSesion(String sesionId) {
        this.stripeSesionId = sesionId;
    }

    public void markPagado(String pagoIntentId, String subscriptionId, OffsetDateTime periodFin) {
        this.status = "PAID";
        this.stripePagoIntentId = pagoIntentId;
        this.stripeSubscriptionId = subscriptionId;
        this.periodFin = periodFin;
        this.fechaPago = OffsetDateTime.now();
    }

    public void markFailed() {
        this.status = "FAILED";
    }

    public void markRefunded() {
        this.status = "REFUNDED";
    }
}
