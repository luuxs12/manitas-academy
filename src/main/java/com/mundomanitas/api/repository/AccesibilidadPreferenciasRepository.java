package com.mundomanitas.api.repository;

import com.mundomanitas.api.model.AccesibilidadPreferencias;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccesibilidadPreferenciasRepository
        extends JpaRepository<AccesibilidadPreferencias, UUID> {
}
