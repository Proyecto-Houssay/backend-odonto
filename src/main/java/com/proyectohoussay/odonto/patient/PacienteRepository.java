package com.proyectohoussay.odonto.patient;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    boolean existsByDni(String dni);
}