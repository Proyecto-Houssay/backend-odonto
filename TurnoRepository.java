package com.proyectohoussay.backendodonto;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;

public interface TurnoRepository extends JpaRepository<Turno, Long> {

    boolean existsByOdontologoIdAndFechaAndHorarioAndEstadoNot(
            Long odontologoId,
            LocalDate fecha,
            LocalTime horario,
            EstadoTurno estado
    );
}