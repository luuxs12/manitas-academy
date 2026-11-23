package com.mundomanitas.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.mundomanitas.api.actuator.BusinessMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mundomanitas.api.config.StripeProperties;
import com.mundomanitas.api.exception.PaymentUnavailableException;
import com.mundomanitas.api.model.Payment;
import com.mundomanitas.api.model.Plan;
import com.mundomanitas.api.model.User;
import com.mundomanitas.api.repository.PaymentRepository;
import com.mundomanitas.api.repository.UserRepository;
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
class PaymentServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Mock
    private StripeProperties properties;

    @Mock
    private StripeClient stripe;

    @Mock
    private PaymentRepository payments;

    @Mock
    private UserRepository users;

    @Mock
    private NotificationService notifications;

    @Mock
    private BusinessMetrics metrics;

    @Mock
    private User user;

    private PaymentService service;

    @BeforeEach
    void setUp() {
        service = new PaymentServiceImpl(
                properties, stripe, payments, users, notifications, metrics);
    }

    @Test
    @DisplayName("rechaza un plan que no requiere pago")
    void rejectsFreePlan() {
        assertThatThrownBy(() -> service.checkout(user, Plan.FREE, "ok", "cancel"))
                .isInstanceOf(PaymentUnavailableException.class);

        verifyNoInteractions(stripe, payments);
    }

    @Test
    @DisplayName("no inicia checkout si faltan credenciales")
    void rejectsCheckoutWhenDisabled() {
        when(properties.isEnabled()).thenReturn(false);

        assertThatThrownBy(() -> service.checkout(user, Plan.MONTHLY, "ok", "cancel"))
                .isInstanceOf(PaymentUnavailableException.class)
                .hasMessageContaining("no está habilitado");

        verifyNoInteractions(stripe, payments);
    }

    @Test
    @DisplayName("guarda el pago pendiente con la sesión de Stripe")
    void createsPendingPayment() {
        when(properties.isEnabled()).thenReturn(true);
        when(stripe.createCheckoutSession(user, Plan.MONTHLY, "ok", "cancel"))
                .thenReturn(new StripeClient.Session("cs_123", "https://checkout.stripe.test/1"));
        when(payments.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String url = service.checkout(user, Plan.MONTHLY, "ok", "cancel");

        assertThat(url).isEqualTo("https://checkout.stripe.test/1");
        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(payments).save(captor.capture());
        assertThat(captor.getValue().getPlan()).isEqualTo(Plan.MONTHLY);
        assertThat(captor.getValue().getStripeSessionId()).isEqualTo("cs_123");
        assertThat(captor.getValue().isPending()).isTrue();
    }

    @Test
    @DisplayName("activa el plan al volver de una sesión pagada")
    void confirmsPaidSession() throws Exception {
        UUID userId = UUID.randomUUID();
        when(user.getId()).thenReturn(userId);
        when(properties.isEnabled()).thenReturn(true);
        when(stripe.retrieveSession("cs_123")).thenReturn(mapper.readTree("""
                {
                  "id": "cs_123",
                  "payment_status": "paid",
                  "metadata": {"user_id": "%s", "plan": "MONTHLY"},
                  "subscription": "sub_123",
                  "payment_intent": "pi_123",
                  "customer": "cus_123"
                }
                """.formatted(userId)));
        when(stripe.retrieveSubscription("sub_123")).thenReturn(mapper.readTree("""
                {"id": "sub_123", "current_period_end": 1767225600}
                """));
        when(payments.findByStripeSessionId("cs_123")).thenReturn(Optional.empty());
        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(payments.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.confirmAfterReturn(user, "cs_123");

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(payments, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getValue().isPaid()).isTrue();
        assertThat(captor.getValue().getStripeSubscriptionId()).isEqualTo("sub_123");
        assertThat(captor.getValue().getPeriodEnd())
                .isEqualTo(OffsetDateTime.ofInstant(Instant.ofEpochSecond(1767225600), ZoneOffset.UTC));
        verify(user).activateSubscription(eq(Plan.MONTHLY), any(OffsetDateTime.class));
        verify(user).changeStripeCustomerId("cus_123");
        verify(notifications).notify(any(User.class), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("marca como fallido un checkout vencido")
    void marksExpiredCheckoutAsFailed() throws Exception {
        JsonNode event = mapper.readTree("""
                {"type": "checkout.session.expired", "data": {"object": {"id": "cs_123"}}}
                """);
        Payment pending = mock(Payment.class);
        when(payments.findByStripeSessionId("cs_123")).thenReturn(Optional.of(pending));

        service.handleEvent(event);

        verify(pending).markFailed();
        verify(payments).save(pending);
        verify(users, never()).save(any());
    }
}
