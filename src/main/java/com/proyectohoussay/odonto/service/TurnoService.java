package com.proyectohoussay.odonto.service;

import com.proyectohoussay.odonto.model.Turno;
import com.proyectohoussay.odonto.repository.TurnoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class TurnoService {

    private final TurnoRepository turnoRepository;

    public TurnoService(TurnoRepository turnoRepository) {
        this.turnoRepository = turnoRepository;
    }

    public List<Turno> listarTurnos() {
        return turnoRepository.findAll();
    }

    public Turno obtenerTurno(Long id) {
        return turnoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Turno no encontrado con ID: " + id));
    }

    public List<Turno> listarPorFecha(LocalDate fecha) {
        return turnoRepository.findByFecha(fecha);
    }

    public List<Turno> listarPorOdontologo(Long odontologoId) {
        return turnoRepository.findByOdontologoId(odontologoId);
    }

    public List<Turno> listarPorPaciente(Long pacienteId) {
        return turnoRepository.findByPacienteId(pacienteId);
    }

    public boolean comprobarDisponibilidad(Long odontologoId, LocalDate fecha, LocalTime hora) {
        return !turnoRepository.existsByOdontologoIdAndFechaAndHora(odontologoId, fecha, hora);
    }

    public Turno crearTurno(Turno turno) {
        if (turno.getOdontologo() != null && turno.getOdontologo().getId() != null) {
            boolean disponible = comprobarDisponibilidad(turno.getOdontologo().getId(), turno.getFecha(), turno.getHora());
            if (!disponible) {
                throw new IllegalStateException("El odontólogo no tiene disponibilidad en la fecha y hora seleccionadas.");
            }
        }
        return turnoRepository.save(turno);
    }

    public Turno actualizarTurno(Long id, Turno details) {
        Turno turno = obtenerTurno(id);
        turno.setFecha(details.getFecha());
        turno.setHora(details.getHora());
        turno.setMotivo(details.getMotivo());
        turno.setEstado(details.getEstado());
        turno.setPaciente(details.getPaciente());
        turno.setOdontologo(details.getOdontologo());
        return turnoRepository.save(turno);
    }

    public void cancelarTurno(Long id) {
        Turno turno = obtenerTurno(id);
        turno.setEstado("CANCELADO");
        turnoRepository.save(turno);
    }
}
