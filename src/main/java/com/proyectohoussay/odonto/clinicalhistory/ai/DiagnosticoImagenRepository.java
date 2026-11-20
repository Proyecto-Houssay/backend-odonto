package com.proyectohoussay.odonto.clinicalhistory.ai;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiagnosticoImagenRepository extends JpaRepository<DiagnosticoImagen, Long> {

    List<DiagnosticoImagen> findAllByPacienteIdOrderByFechaAnalisisDesc(Long pacienteId);

    List<DiagnosticoImagen> findAllByHistoriaClinicaIdOrderByFechaAnalisisDesc(Long historiaClinicaId);
}
