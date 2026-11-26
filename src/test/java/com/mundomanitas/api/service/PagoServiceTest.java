package com.mundomanitas.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.mundomanitas.api.actuator.BusinessMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mundomanitas.api.config.StripeProperties;
import com.mundomanitas.api.exception.PagoNoDisponibleException;
import com.mundomanitas.api.model.Pago;
import com.mundomanitas.api.model.Plan;
import com.mundomanitas.api.model.Usuario;
import com.mundomanitas.api.repository.PagoRepository;
import com.mundomanitas.api.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Cobros de membresía")
class PagoServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Mock
    private StripeProperties properties;

    @Mock
    private StripeClient stripe;

    @Mock
    private PagoRepository payments;

    @Mock
    private UsuarioRepository users;

    @Mock
    private NotificacionService notifications;

    @Mock
    private BusinessMetrics metrics;

    @Mock
    private Usuario usuario;

    private PagoService service;

    @BeforeEach
    void setUp() {
        service = new PagoServiceImpl(
                properties, stripe, payments, users, notifications, metrics);
    }

    @Test
    @DisplayName("rechaza un plan que no requiere pago")
    void rejectsFreePlan() {
        assertThatThrownBy(() -> service.checkout(usuario, Plan.FREE, "ok", "cancel"))
                .isInstanceOf(PagoNoDisponibleException.class);

        verifyNoInteractions(stripe, payments);
    }

    @Test
    @DisplayName("no inicia checkout si faltan credenciales")
    void rejectsCheckoutWhenDisabled() {
        when(properties.isEnabled()).thenReturn(false);

        assertThatThrownBy(() -> service.checkout(usuario, Plan.MONTHLY, "ok", "cancel"))
                .isInstanceOf(PagoNoDisponibleException.class)
                .hasMessageContaining("no está habilitado");

        verifyNoInteractions(stripe, payments);
    }

    @Test
    @DisplayName("guarda el pago pendiente con la sesión de Stripe")
    void createsPendingPago() {
        when(properties.isEnabled()).thenReturn(true);
        when(stripe.createCheckoutSesion(usuario, Plan.MONTHLY, "ok", "cancel"))
                .thenReturn(new StripeClient.Sesion("cs_123", "https://checkout.stripe.test/1"));
        when(payments.save(any(Pago.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String url = service.checkout(usuario, Plan.MONTHLY, "ok", "cancel");

        assertThat(url).isEqualTo("https://checkout.stripe.test/1");
        ArgumentCaptor<Pago> captor = ArgumentCaptor.forClass(Pago.class);
        verify(payments).save(captor.capture());
        assertThat(captor.getValue().getPlan()).isEqualTo(Plan.MONTHLY);
        assertThat(captor.getValue().getStripeSesionId()).isEqualTo("cs_123");
        assertThat(captor.getValue().isPending()).isTrue();
    }

    @Test
    @DisplayName("activa el plan al volver de una sesión pagada")
    void confirmsPagadoSesion() throws Exception {
        UUID usuarioId = UUID.randomUUID();
        when(usuario.getId()).thenReturn(usuarioId);
        when(properties.isEnabled()).thenReturn(true);
        when(stripe.retrieveSesion("cs_123")).thenReturn(mapper.readTree("""
                {
                  "id": "cs_123",
                  "payment_status": "paid",
                  "metadata": {"user_id": "%s", "plan": "MONTHLY"},
                  "subscription": "sub_123",
                  "payment_intent": "pi_123",
                  "customer": "cus_123"
                }
                """.formatted(usuarioId)));
        when(stripe.retrieveSubscription("sub_123")).thenReturn(mapper.readTree("""
                {"id": "sub_123", "current_period_end": 1767225600}
                """));
        when(payments.buscarPorStripeSesionId("cs_123")).thenReturn(Optional.empty());
        when(users.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(payments.save(any(Pago.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.confirmDespuesReturn(usuario, "cs_123");

        ArgumentCaptor<Pago> captor = ArgumentCaptor.forClass(Pago.class);
        verify(payments, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getValue().isPagado()).isTrue();
        assertThat(captor.getValue().getStripeSubscriptionId()).isEqualTo("sub_123");
        assertThat(captor.getValue().getPeriodFin())
                .isEqualTo(OffsetDateTime.ofInstant(Instant.ofEpochSecond(1767225600), ZoneOffset.UTC));
        verify(usuario).activateSubscription(eq(Plan.MONTHLY), any(OffsetDateTime.class));
        verify(usuario).changeStripeCustomerId("cus_123");
        verify(notifications).notify(any(Usuario.class), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("marca como fallido un checkout vencido")
    void marksExpiredCheckoutAsFailed() throws Exception {
        JsonNode evento = mapper.readTree("""
                {"type": "checkout.session.expired", "data": {"object": {"id": "cs_123"}}}
                """);
        Pago pending = mock(Pago.class);
        when(payments.buscarPorStripeSesionId("cs_123")).thenReturn(Optional.of(pending));

        service.handleEvento(evento);

        verify(pending).markFailed();
        verify(payments).save(pending);
        verify(users, never()).save(any());
    }
}
