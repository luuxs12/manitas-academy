package com.mundomanitas.api.repository;

import com.mundomanitas.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Consultas de cuentas. El email se guarda en minúsculas, pero se busca
 * sin distinguir mayúsculas para no duplicar personas por una mayúscula.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
