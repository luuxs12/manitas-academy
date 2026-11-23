package com.mundomanitas.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.mundomanitas.api.model.Payment;
import com.mundomanitas.api.model.Plan;
import com.mundomanitas.api.model.User;

import java.util.List;

/**
 * Cobros de la membresía.
 *
 * <p>El plan solo cambia cuando la pasarela confirma un pago. Sin
 * credenciales el checkout se apaga: no hay botones que simulen una compra
 * ni planes activados a mano.</p>
 */
public interface PaymentService {

    boolean isEnabled();

    boolean isEmbeddedEnabled();

    String publishableKey();

    String customerPortalUrl();

    List<Payment> history(User user);

    String checkout(User user, Plan plan, String successUrl, String cancelUrl);

    EmbeddedCheckout embeddedCheckout(User user, Plan plan, String returnUrl);

    void confirmAfterReturn(User user, String sessionId);

    void handleEvent(JsonNode event);

    void cancelAtPeriodEnd(User user);

    record EmbeddedCheckout(String clientSecret, String sessionId) {
    }
}
