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
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Plan plan;

    @Column(name = "amount_cents", nullable = false)
    private int amountCents;

    @Column(nullable = false)
    private String currency = "PEN";

    @Column(nullable = false)
    private String status = "PENDING";

    @Column(name = "stripe_session_id", unique = true)
    private String stripeSessionId;

    @Column(name = "stripe_payment_intent_id")
    private String stripePaymentIntentId;

    @Column(name = "stripe_subscription_id")
    private String stripeSubscriptionId;

    @Column(name = "period_end")
    private OffsetDateTime periodEnd;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    protected Payment() {
    }

    public Payment(User user, Plan plan) {
        this.user = user;
        this.plan = plan;
        this.amountCents = plan.amountCents();
    }

    /** Monto informado por Stripe, que puede incluir impuestos del cobro real. */
    public Payment(User user, Plan plan, int amountCents) {
        this(user, plan);
        if (amountCents > 0) {
            this.amountCents = amountCents;
        }
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
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

    public String getStatus() {
        return status;
    }

    public String getStripeSessionId() {
        return stripeSessionId;
    }

    public String getStripeSubscriptionId() {
        return stripeSubscriptionId;
    }

    public OffsetDateTime getPeriodEnd() {
        return periodEnd;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getPaidAt() {
        return paidAt;
    }

    public boolean isPaid() {
        return "PAID".equals(status);
    }

    public boolean isPending() {
        return "PENDING".equals(status);
    }

    public void linkSession(String sessionId) {
        this.stripeSessionId = sessionId;
    }

    public void markPaid(String paymentIntentId, String subscriptionId, OffsetDateTime periodEnd) {
        this.status = "PAID";
        this.stripePaymentIntentId = paymentIntentId;
        this.stripeSubscriptionId = subscriptionId;
        this.periodEnd = periodEnd;
        this.paidAt = OffsetDateTime.now();
    }

    public void markFailed() {
        this.status = "FAILED";
    }

    public void markRefunded() {
        this.status = "REFUNDED";
    }
}
