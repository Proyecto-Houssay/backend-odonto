package com.proyectohoussay.odonto.repository;

import com.proyectohoussay.odonto.model.Turno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface TurnoRepository extends JpaRepository<Turno, Long> {

    List<Turno> findByFecha(LocalDate fecha);

    List<Turno> findByOdontologoId(Long odontologoId);

    List<Turno> findByPacienteId(Long pacienteId);

    List<Turno> findByFechaAndOdontologoId(LocalDate fecha, Long odontologoId);

    boolean existsByOdontologoIdAndFechaAndHora(Long odontologoId, LocalDate fecha, LocalTime hora);
}
