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
public class CorreoProperties {

    /** Clave de Resend (empieza con {@code re_}). */
    private String apiClave = "";

    /** Remitente verificado, por ejemplo {@code Mundo Manitas <hola@dominio>}. */
    private String from = "Mundo Manitas <onboarding@resend.dev>";

    /** URL pública del sitio para armar los enlaces de los correos. */
    private String baseUrl = "http://localhost:8080";

    /** Raíz de la API de Resend; se puede cambiar en pruebas. */
    private String endpoint = "https://api.resend.com";

    /** Minutos que vive un enlace de restablecimiento. */
    private long restablecerMinutes = 60;

    public String getApiClave() {
        return apiClave;
    }

    public void setApiClave(String apiClave) {
        this.apiClave = apiClave;
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

    public long getRestablecerMinutes() {
        return restablecerMinutes;
    }

    public void setRestablecerMinutes(long restablecerMinutes) {
        this.restablecerMinutes = restablecerMinutes;
    }

    public boolean isEnabled() {
        return apiClave != null && !apiClave.isBlank();
    }
}
