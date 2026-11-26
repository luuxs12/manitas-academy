package com.mundomanitas.api.config;

import com.mundomanitas.api.model.Plan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuración de Stripe. La clave y los price ids llegan por entorno;
 * sin ellos el checkout queda apagado y la interfaz lo dice abiertamente.
 */
@Component
@ConfigurationProperties(prefix = "app.stripe")
public class StripeProperties {

    private String secretClave = "";
    private String publishableClave = "";
    private String webhookSecret = "";
    private String monthlyPriceId = "";
    private String annualPriceId = "";
    private String endpoint = "https://api.stripe.com";
    private String apiVersion = "2026-11-18.endive";
    private String customerPortalUrl = "";

    public String getSecretClave() {
        return secretClave;
    }

    public void setSecretClave(String secretClave) {
        this.secretClave = secretClave;
    }

    public String getPublishableClave() {
        return publishableClave;
    }

    public void setPublishableClave(String publishableClave) {
        this.publishableClave = publishableClave;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public String getMonthlyPriceId() {
        return monthlyPriceId;
    }

    public void setMonthlyPriceId(String monthlyPriceId) {
        this.monthlyPriceId = monthlyPriceId;
    }

    public String getAnnualPriceId() {
        return annualPriceId;
    }

    public void setAnnualPriceId(String annualPriceId) {
        this.annualPriceId = annualPriceId;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getCustomerPortalUrl() {
        return customerPortalUrl;
    }

    public void setCustomerPortalUrl(String customerPortalUrl) {
        this.customerPortalUrl = customerPortalUrl;
    }

    public boolean isEnabled() {
        return !secretClave.isBlank()
                && !monthlyPriceId.isBlank()
                && !annualPriceId.isBlank();
    }

    public String priceId(Plan plan) {
        return plan == Plan.ANNUAL ? annualPriceId : monthlyPriceId;
    }
}
