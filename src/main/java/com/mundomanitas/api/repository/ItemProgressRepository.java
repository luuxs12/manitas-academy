package com.mundomanitas.api.repository;

import com.mundomanitas.api.model.ItemProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ItemProgressRepository
        extends JpaRepository<ItemProgress, ItemProgress.Key> {

    List<ItemProgress> findByEnrollmentIdOrderByCompletedAtAsc(UUID enrollmentId);

    long countByEnrollmentId(UUID enrollmentId);
}
