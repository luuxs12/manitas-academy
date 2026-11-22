package com.mundomanitas.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Cuenta de una persona en la plataforma.
 *
 * <p>La tabla la crea Flyway en {@code V1__accounts.sql}. Hibernate solo
 * valida que el mapeo coincida; nunca modifica el esquema.</p>
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private String locale;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Plan plan = Plan.FREE;

    @Column(name = "avatar_path")
    private String avatarPath;

    @Column(nullable = false)
    private String theme = "SYSTEM";

    @Column(name = "notify_email", nullable = false)
    private boolean notifyEmail = true;

    @Column(name = "stripe_customer_id", unique = true)
    private String stripeCustomerId;

    @Column(name = "subscription_end")
    private OffsetDateTime subscriptionEnd;

    @Column(name = "cancel_at_period_end", nullable = false)
    private boolean cancelAtPeriodEnd;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    /** Constructor vacío que exige JPA. */
    protected User() {
    }

    public User(String email, String passwordHash, String fullName) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = "STUDENT";
        this.active = true;
        this.locale = "es-PE";
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    /** Cambia la contraseña ya cifrada; la regla de largo vive en el servicio. */
    public void changePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public String getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public String getLocale() {
        return locale;
    }

    public Plan getPlan() {
        return plan;
    }

    public boolean isPremium() {
        return plan.isPremium();
    }

    public void changeFullName(String fullName) {
        this.fullName = fullName;
    }

    public void changeEmail(String email) {
        this.email = email;
    }

    /** Cambia el rol desde el panel administrativo; los valores los valida la BD. */
    public void changeRole(String role) {
        this.role = role;
    }

    public void changeActive(boolean active) {
        this.active = active;
    }

    public String getAvatarPath() {
        return avatarPath;
    }

    public void changeAvatarPath(String avatarPath) {
        this.avatarPath = avatarPath;
    }

    public String getTheme() {
        return theme;
    }

    public boolean isNotifyEmail() {
        return notifyEmail;
    }

    public String getStripeCustomerId() {
        return stripeCustomerId;
    }

    public OffsetDateTime getSubscriptionEnd() {
        return subscriptionEnd;
    }

    public boolean isCancelAtPeriodEnd() {
        return cancelAtPeriodEnd;
    }

    public void changePreferences(String theme, boolean notifyEmail) {
        this.theme = theme;
        this.notifyEmail = notifyEmail;
    }

    public void changeStripeCustomerId(String stripeCustomerId) {
        this.stripeCustomerId = stripeCustomerId;
    }

    /** Activa el plan pagado con el fin de período informado por Stripe. */
    public void activateSubscription(Plan plan, OffsetDateTime periodEnd) {
        this.plan = plan;
        this.subscriptionEnd = periodEnd;
        this.cancelAtPeriodEnd = false;
    }

    public void cancelSubscriptionAtPeriodEnd() {
        this.cancelAtPeriodEnd = true;
    }

    public void updateSubscriptionEnd(OffsetDateTime periodEnd) {
        this.subscriptionEnd = periodEnd;
    }

    public void expireSubscription() {
        this.plan = Plan.FREE;
        this.subscriptionEnd = null;
        this.cancelAtPeriodEnd = false;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
