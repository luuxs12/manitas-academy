package com.mundomanitas.api.service;

import com.mundomanitas.api.config.MailProperties;
import com.mundomanitas.api.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.HtmlUtils;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Envía los correos transaccionales con la API REST de Resend.
 *
 * <p>No usa SMTP: un POST a {@code /emails} con la clave en el encabezado.
 * Si no hay clave configurada, el envío se omite y se anota en el log; así
 * el registro y el resto del sitio funcionan en desarrollo.</p>
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final MailProperties properties;
    private final RestClient client;

    public MailService(MailProperties properties) {
        this.properties = properties;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(4));
        requestFactory.setReadTimeout(Duration.ofSeconds(8));

        this.client = RestClient.builder()
                .baseUrl(properties.getEndpoint())
                .requestFactory(requestFactory)
                .build();
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    /** Aviso de cuenta creada. Un fallo de correo nunca frena el registro. */
    public void sendWelcome(User user) {
        String panelUrl = baseUrl() + "/panel";
        String name = HtmlUtils.htmlEscape(firstName(user.getFullName()));

        String html = layout(
                "Tu cuenta está lista",
                """
                <h1 style="margin:0 0 16px;font-size:28px;line-height:1.15;color:#000000;">¡Hola, {{NAME}}!</h1>
                <p style="margin:0 0 16px;font-size:16px;line-height:1.6;color:#000000;">
                  Tu cuenta en Mundo Manitas quedó creada. Ya podés entrar a tu panel,
                  ver tus cursos y empezar cuando quieras.
                </p>
                <p style="margin:0 0 24px;font-size:16px;line-height:1.6;color:#000000;">
                  Los cursos gratuitos no vencen y cada clase tiene intérprete en
                  Lengua de Señas Peruana y subtítulos revisados.
                </p>
                <p style="margin:0 0 24px;">
                  <a href="{{PANEL}}" style="display:inline-block;padding:14px 26px;border-radius:999px;background:#00FF00;color:#000000;font-weight:700;text-decoration:none;">Ir a mi panel</a>
                </p>
                <p style="margin:0;font-size:14px;line-height:1.6;color:#555555;">
                  Si el botón no abre, copiá este enlace en tu navegador:<br>
                  <a href="{{PANEL}}" style="color:#000000;">{{PANEL}}</a>
                </p>
                """
        )
                .replace("{{NAME}}", name)
                .replace("{{PANEL}}", panelUrl);

        String text = """
                ¡Hola, %s!

                Tu cuenta en Mundo Manitas quedó creada. Ya podés entrar a tu panel,
                ver tus cursos y empezar cuando quieras.

                Entrá desde acá: %s

                Los cursos gratuitos no vencen y cada clase tiene intérprete en
                Lengua de Señas Peruana y subtítulos revisados.
                """.formatted(firstName(user.getFullName()), panelUrl);

        send(user.getEmail(), "Tu cuenta en Mundo Manitas está lista", html, text);
    }

    /** Enlace de un solo uso para elegir una contraseña nueva. */
    public void sendPasswordReset(User user, String token) {
        String resetUrl = baseUrl() + "/restablecer?token=" + token;
        String name = HtmlUtils.htmlEscape(firstName(user.getFullName()));

        String html = layout(
                "Restablecé tu contraseña",
                """
                <h1 style="margin:0 0 16px;font-size:28px;line-height:1.15;color:#000000;">Hola, {{NAME}}</h1>
                <p style="margin:0 0 16px;font-size:16px;line-height:1.6;color:#000000;">
                  Recibimos un pedido para cambiar la contraseña de tu cuenta.
                  Elegí una nueva desde este enlace:
                </p>
                <p style="margin:0 0 24px;">
                  <a href="{{RESET}}" style="display:inline-block;padding:14px 26px;border-radius:999px;background:#00FF00;color:#000000;font-weight:700;text-decoration:none;">Elegir contraseña nueva</a>
                </p>
                <p style="margin:0 0 16px;font-size:14px;line-height:1.6;color:#555555;">
                  El enlace vence en {{MINUTES}} minutos y sirve una sola vez.
                </p>
                <p style="margin:0;font-size:14px;line-height:1.6;color:#555555;">
                  Si no pediste el cambio, ignorá este correo: tu contraseña sigue igual.
                </p>
                """
        )
                .replace("{{NAME}}", name)
                .replace("{{RESET}}", resetUrl)
                .replace("{{MINUTES}}", Long.toString(properties.getResetMinutes()));

        String text = """
                Hola, %s

                Recibimos un pedido para cambiar la contraseña de tu cuenta.
                Elegí una nueva desde este enlace:

                %s

                El enlace vence en %d minutos y sirve una sola vez.
                Si no pediste el cambio, ignorá este correo: tu contraseña sigue igual.
                """.formatted(firstName(user.getFullName()), resetUrl, properties.getResetMinutes());

        send(user.getEmail(), "Restablecé tu contraseña de Mundo Manitas", html, text);
    }

    private void send(String to, String subject, String html, String text) {
        if (!properties.isEnabled()) {
            log.info("Correo omitido (falta RESEND_API_KEY): {} para {}", subject, to);
            return;
        }

        Map<String, Object> payload = Map.of(
                "from", properties.getFrom(),
                "to", List.of(to),
                "subject", subject,
                "html", html,
                "text", text
        );

        try {
            client.post()
                    .uri("/emails")
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Correo enviado con Resend: {}", subject);
        } catch (RestClientException exception) {
            // La cuenta ya está creada o el token ya está guardado: el correo
            // es un extra, no la operación principal.
            log.warn("Resend no pudo enviar «{}»: {}", subject, exception.getMessage());
        }
    }

    private String baseUrl() {
        String base = properties.getBaseUrl();
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    private String firstName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "hola";
        }
        return fullName.trim().split("\\s+")[0];
    }

    /**
     * Marco común de los correos. Estilos en línea y sin imágenes externas:
     * los clientes de correo suelen bloquearlas.
     */
    private String layout(String title, String body) {
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>{{TITLE}}</title>
                </head>
                <body style="margin:0;padding:0;background:#F5F5F3;">
                  <div style="padding:28px 16px;">
                    <div style="max-width:560px;margin:0 auto;background:#FFFFFF;border-radius:20px;overflow:hidden;">
                      <div style="padding:18px 28px;background:#00FF00;color:#000000;font-size:18px;font-weight:800;letter-spacing:-0.01em;">
                        Mundo Manitas
                      </div>
                      <div style="padding:32px 28px 28px;font-family:Arial,Helvetica,sans-serif;">
                        {{BODY}}
                      </div>
                      <div style="padding:18px 28px;border-top:1px solid #E6E6E3;font-family:Arial,Helvetica,sans-serif;font-size:12px;line-height:1.6;color:#666666;">
                        Mundo Manitas · Cursos de tecnología en Lengua de Señas Peruana<br>
                        Hecho en Perú. Este correo se envió porque alguien usó este buzón en mundomanitas.com.
                      </div>
                    </div>
                  </div>
                </body>
                </html>
                """
                .replace("{{TITLE}}", HtmlUtils.htmlEscape(title))
                .replace("{{BODY}}", body);
    }
}
