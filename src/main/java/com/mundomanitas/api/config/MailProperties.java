package com.mundomanitas.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Datos de envío con Resend.
 *
 * <p>La clave no se guarda en el repositorio: llega por la variable de
 * entorno {@code RESEND_API_KEY}. Sin clave, el correo queda apagado y el
 * resto del sitio sigue funcionando.</p>
 */
@Component
@ConfigurationProperties(prefix = "app.mail")
public class MailProperties {

    /** Clave de Resend (empieza con {@code re_}). */
    private String apiKey = "";

    /** Remitente verificado, por ejemplo {@code Mundo Manitas <hola@dominio>}. */
    private String from = "Mundo Manitas <onboarding@resend.dev>";

    /** URL pública del sitio para armar los enlaces de los correos. */
    private String baseUrl = "http://localhost:8080";

    /** Raíz de la API de Resend; se puede cambiar en pruebas. */
    private String endpoint = "https://api.resend.com";

    /** Minutos que vive un enlace de restablecimiento. */
    private long resetMinutes = 60;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public long getResetMinutes() {
        return resetMinutes;
    }

    public void setResetMinutes(long resetMinutes) {
        this.resetMinutes = resetMinutes;
    }

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }
}
