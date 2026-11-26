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
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String contrasenaHash;

    @Column(name = "full_name", nullable = false)
    private String nombreCompleto;

    @Column(name = "role", nullable = false)
    private String rol;

    @Column(name = "active", nullable = false)
    private boolean activo;

    @Column(name = "locale", nullable = false)
    private String locale;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan", nullable = false)
    private Plan plan = Plan.FREE;

    @Column(name = "avatar_path")
    private String avatarPath;

    @Column(name = "theme", nullable = false)
    private String theme = "SYSTEM";

    @Column(name = "notify_email", nullable = false)
    private boolean notifyEmail = true;

    @Column(name = "stripe_customer_id", unique = true)
    private String stripeCustomerId;

    @Column(name = "subscription_end")
    private OffsetDateTime subscriptionFin;

    @Column(name = "cancel_at_period_end", nullable = false)
    private boolean cancelAtPeriodFin;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime fechaActualizacion;

    /** Constructor vacío que exige JPA. */
    protected Usuario() {
    }

    public Usuario(String email, String contrasenaHash, String nombreCompleto) {
        this.email = email;
        this.contrasenaHash = contrasenaHash;
        this.nombreCompleto = nombreCompleto;
        this.rol = "STUDENT";
        this.activo = true;
        this.locale = "es-PE";
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getContrasenaHash() {
        return contrasenaHash;
    }

    /** Cambia la contraseña ya cifrada; la regla de largo vive en el servicio. */
    public void changeContrasenaHash(String contrasenaHash) {
        this.contrasenaHash = contrasenaHash;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public String getRol() {
        return rol;
    }

    public boolean isActivo() {
        return activo;
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

    public void changeCompletoNombre(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public void changeEmail(String email) {
        this.email = email;
    }

    /** Cambia el rol desde el panel administrativo; los valores los valida la BD. */
    public void changeRol(String rol) {
        this.rol = rol;
    }

    public void changeActivo(boolean activo) {
        this.activo = activo;
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

    public OffsetDateTime getSubscriptionFin() {
        return subscriptionFin;
    }

    public boolean isCancelAtPeriodFin() {
        return cancelAtPeriodFin;
    }

    public void changePreferencias(String theme, boolean notifyEmail) {
        this.theme = theme;
        this.notifyEmail = notifyEmail;
    }

    public void changeStripeCustomerId(String stripeCustomerId) {
        this.stripeCustomerId = stripeCustomerId;
    }

    /** Activa el plan pagado con el fin de período informado por Stripe. */
    public void activateSubscription(Plan plan, OffsetDateTime periodFin) {
        this.plan = plan;
        this.subscriptionFin = periodFin;
        this.cancelAtPeriodFin = false;
    }

    public void cancelSubscriptionAtPeriodFin() {
        this.cancelAtPeriodFin = true;
    }

    public void updateSubscriptionFin(OffsetDateTime periodFin) {
        this.subscriptionFin = periodFin;
    }

    public void expiraSubscription() {
        this.plan = Plan.FREE;
        this.subscriptionFin = null;
        this.cancelAtPeriodFin = false;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public OffsetDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }
}
