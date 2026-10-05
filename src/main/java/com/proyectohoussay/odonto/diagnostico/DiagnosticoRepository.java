package com.proyectohoussay.odonto.diagnostico;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiagnosticoRepository extends JpaRepository<Diagnostico, Long> {

    List<Diagnostico> findAllByHistoriaClinicaId(Long historiaClinicaId);
}
