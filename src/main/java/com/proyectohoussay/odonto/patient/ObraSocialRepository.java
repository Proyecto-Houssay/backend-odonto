package com.proyectohoussay.odonto.patient;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ObraSocialRepository extends JpaRepository<ObraSocial, Long> {
    Optional<ObraSocial> findByCodigoIgnoreCase(String codigo);
    boolean existsByCodigoIgnoreCase(String codigo);
}
