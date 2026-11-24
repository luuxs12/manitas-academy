package com.mundomanitas.api.repository;

import com.mundomanitas.api.model.Payment;
import com.mundomanitas.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByUserOrderByCreatedAtDesc(User user);

    Optional<Payment> findByStripeSessionId(String sessionId);

    Optional<Payment> findByStripePaymentIntentId(String paymentIntentId);

    Optional<Payment> findByStripeSubscriptionId(String subscriptionId);

    @Query("select coalesce(sum(p.amountCents), 0) from Payment p where p.status = 'PAID'")
    long paidTotalCents();

    long countByStatusAndPaidAtBetween(String status, OffsetDateTime from, OffsetDateTime to);

    List<Payment> findTop5ByStatusOrderByPaidAtDesc(String status);
}
