package com.mundomanitas.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mundomanitas.api.config.StripeProperties;
import com.mundomanitas.api.model.Plan;
import com.mundomanitas.api.model.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;

/** Cliente HTTP de Stripe: solo Checkout Sessions y webhooks. */
@Service
public class StripeClient {

    private static final Logger log = LoggerFactory.getLogger(StripeClient.class);

    private final StripeProperties properties;
    private final RestClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    public StripeClient(StripeProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(4));
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.client = RestClient.builder()
                .baseUrl(properties.getEndpoint())
                .requestFactory(factory)
                .build();
    }

    /** Devuelve la URL de pago y el id de sesión (flujo heredado). */
    public Sesion createCheckoutSesion(Usuario usuario, Plan plan, String successUrl, String cancelUrl) {
        MultiValueMap<String, String> formulario = baseSesionFormulario(usuario, plan);
        formulario.add("success_url", successUrl);
        formulario.add("cancel_url", cancelUrl);

        JsonNode json = postSesion(formulario);
        return new Sesion(json.path("id").asText(), json.path("url").asText());
    }

    /**
     * Checkout embebido: Stripe devuelve un client_secret que se monta dentro
     * del panel, sin sacar a la persona de la plataforma.
     */
    public EmbeddedSesion createEmbeddedCheckoutSesion(Usuario usuario, Plan plan, String returnUrl) {
        MultiValueMap<String, String> formulario = baseSesionFormulario(usuario, plan);
        formulario.add("ui_mode", "embedded_page");
        formulario.add("return_url", returnUrl);

        JsonNode json = postSesion(formulario);
        return new EmbeddedSesion(
                json.path("id").asText(),
                json.path("client_secret").asText()
        );
    }

    private MultiValueMap<String, String> baseSesionFormulario(Usuario usuario, Plan plan) {
        MultiValueMap<String, String> formulario = new LinkedMultiValueMap<>();
        formulario.add("mode", "subscription");
        formulario.add("line_items[0][price]", properties.priceId(plan));
        formulario.add("line_items[0][quantity]", "1");
        formulario.add("client_reference_id", usuario.getId().toString());
        formulario.add("customer_email", usuario.getEmail());
        formulario.add("metadata[user_id]", usuario.getId().toString());
        formulario.add("metadata[plan]", plan.name());
        formulario.add("locale", "es");
        if (usuario.getStripeCustomerId() != null && !usuario.getStripeCustomerId().isBlank()) {
            formulario.add("customer", usuario.getStripeCustomerId());
        }
        return formulario;
    }

    private JsonNode postSesion(MultiValueMap<String, String> formulario) {
        String response = client.post()
                .uri("/v1/checkout/sessions")
                .headers(this::authorize)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formulario)
                .retrieve()
                .body(String.class);
        try {
            return mapper.readTree(response);
        } catch (Exception exception) {
            throw new IllegalStateException("Stripe devolvió una sesión inválida", exception);
        }
    }

    public JsonNode retrieveSesion(String sesionId) {
        return get("/v1/checkout/sessions/" + sesionId);
    }

    public JsonNode retrieveSubscription(String subscriptionId) {
        return get("/v1/subscriptions/" + subscriptionId);
    }

    /** Cancela al final del período pagado; Stripe conserva el acceso hasta ahí. */
    public void cancelSubscriptionAtPeriodFin(String subscriptionId) {
        MultiValueMap<String, String> formulario = new LinkedMultiValueMap<>();
        formulario.add("cancel_at_period_end", "true");
        client.post()
                .uri("/v1/subscriptions/" + subscriptionId)
                .headers(this::authorize)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formulario)
                .retrieve()
                .toBodilessEntity();
    }

    private JsonNode get(String path) {
        String response = client.get()
                .uri(path)
                .headers(this::authorize)
                .retrieve()
                .body(String.class);
        try {
            return mapper.readTree(response);
        } catch (Exception exception) {
            throw new IllegalStateException("Stripe devolvió una respuesta inválida", exception);
        }
    }

    private void authorize(HttpHeaders headers) {
        headers.setBearerAuth(properties.getSecretClave());
        if (!properties.getApiVersion().isBlank()) {
            headers.set("Stripe-Version", properties.getApiVersion());
        }
    }

    /** Verifica la firma Stripe-Signature sobre el cuerpo crudo. */
    public boolean validSignature(byte[] body, String header) {
        if (header == null || header.isBlank() || properties.getWebhookSecret().isBlank()) {
            return false;
        }
        try {
            String timestamp = null;
            String provided = null;
            for (String part : header.split(",")) {
                String[] pair = part.trim().split("=", 2);
                if (pair.length != 2) continue;
                if ("t".equals(pair[0])) timestamp = pair[1];
                if ("v1".equals(pair[0])) provided = pair[1];
            }
            if (timestamp == null || provided == null) return false;

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    properties.getWebhookSecret().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            ));
            byte[] payload = (timestamp + ".").getBytes(StandardCharsets.UTF_8);
            byte[] signed = new byte[payload.length + body.length];
            System.arraycopy(payload, 0, signed, 0, payload.length);
            System.arraycopy(body, 0, signed, payload.length, body.length);
            String expected = java.util.HexFormat.of().formatHex(mac.doFinal(signed));
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.US_ASCII),
                    provided.getBytes(StandardCharsets.US_ASCII)
            );
        } catch (Exception exception) {
            log.warn("Firma de Stripe inválida: {}", exception.getMessage());
            return false;
        }
    }

    public JsonNode parse(String raw) {
        try {
            return mapper.readTree(raw);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Cuerpo de webhook inválido", exception);
        }
    }

    public record Sesion(String id, String url) {
    }

    public record EmbeddedSesion(String id, String clientSecret) {
    }
}
