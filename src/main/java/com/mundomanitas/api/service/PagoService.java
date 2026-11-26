package com.mundomanitas.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.mundomanitas.api.model.Pago;
import com.mundomanitas.api.model.Plan;
import com.mundomanitas.api.model.Usuario;

import java.util.List;

/**
 * Cobros de la membresía.
 *
 * <p>El plan solo cambia cuando la pasarela confirma un pago. Sin
 * credenciales el checkout se apaga: no hay botones que simulen una compra
 * ni planes activados a mano.</p>
 */
public interface PagoService {

    boolean isEnabled();

    boolean isEmbeddedEnabled();

    String publishableClave();

    String customerPortalUrl();

    List<Pago> history(Usuario usuario);

    String checkout(Usuario usuario, Plan plan, String successUrl, String cancelUrl);

    EmbeddedCheckout embeddedCheckout(Usuario usuario, Plan plan, String returnUrl);

    void confirmDespuesReturn(Usuario usuario, String sesionId);

    void handleEvento(JsonNode evento);

    void cancelAtPeriodFin(Usuario usuario);

    record EmbeddedCheckout(String clientSecret, String sesionId) {
    }
}
