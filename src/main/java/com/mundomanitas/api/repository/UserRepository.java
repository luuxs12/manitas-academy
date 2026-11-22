package com.mundomanitas.api.repository;

import com.mundomanitas.api.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Consultas de cuentas. El email se guarda en minúsculas, pero se busca
 * sin distinguir mayúsculas para no duplicar personas por una mayúscula.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Query("""
            select u from User u
            where (:role is null or u.role = :role)
              and (:term is null or lower(u.fullName) like lower(concat('%', cast(:term as string), '%'))
                              or lower(u.email) like lower(concat('%', cast(:term as string), '%')))
            order by u.createdAt desc
            """)
    Page<User> search(@Param("role") String role, @Param("term") String term, Pageable pageable);

    List<User> findTop5ByOrderByCreatedAtDesc();

    long countByCreatedAtBetween(OffsetDateTime from, OffsetDateTime to);
}
