package com.mundomanitas.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.mundomanitas.api.service.PagoService;
import com.mundomanitas.api.service.StripeClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint público de Stripe: solo acepta eventos con firma válida. */
@RestController
public class StripeWebhookController {

    private final StripeClient stripe;
    private final PagoService payments;

    public StripeWebhookController(StripeClient stripe, PagoService payments) {
        this.stripe = stripe;
        this.payments = payments;
    }

    @PostMapping("/pagos/stripe/webhook")
    public ResponseEntity<String> webhook(
            @RequestBody byte[] body,
            @RequestHeader(name = "Stripe-Signature", required = false) String signature
    ) {
        if (!stripe.validSignature(body, signature)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Firma inválida");
        }
        JsonNode evento;
        try {
            evento = stripe.parse(new String(body, java.nio.charset.StandardCharsets.UTF_8));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body("Cuerpo inválido");
        }
        payments.handleEvento(evento);
        return ResponseEntity.ok("ok");
    }
}
