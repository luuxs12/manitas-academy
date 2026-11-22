package com.mundomanitas.api.repository;

import com.mundomanitas.api.model.Notification;
import com.mundomanitas.api.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    long countByUserAndReadAtIsNull(User user);

    Optional<Notification> findByIdAndUser(UUID id, User user);
}
