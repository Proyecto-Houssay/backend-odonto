package com.proyectohoussay.odonto.teletriaje.repository;

import com.proyectohoussay.odonto.teletriaje.model.CitaTeletriaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CitaTeletriajeRepository extends JpaRepository<CitaTeletriaje, Long> {

    List<CitaTeletriaje> findByPacienteId(Long pacienteId);

    Optional<CitaTeletriaje> findByTurnoId(Long turnoId);

    Optional<CitaTeletriaje> findBySolicitudId(Long solicitudId);
}
