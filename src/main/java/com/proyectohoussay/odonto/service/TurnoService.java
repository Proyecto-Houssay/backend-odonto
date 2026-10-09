package com.proyectohoussay.odonto.service;

import com.proyectohoussay.odonto.model.Turno;
import com.proyectohoussay.odonto.dto.TurnoRequest;
import com.proyectohoussay.odonto.model.Odontologo;
import com.proyectohoussay.odonto.patient.Paciente;
import com.proyectohoussay.odonto.patient.PacienteRepository;
import com.proyectohoussay.odonto.repository.OdontologoRepository;
import com.proyectohoussay.odonto.repository.TurnoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class TurnoService {

    private static final String FECHA_TURNO_PASADA_MESSAGE = "La fecha del turno no puede ser anterior a la fecha actual.";

    private final TurnoRepository turnoRepository;
    private final PacienteRepository pacienteRepository;
    private final OdontologoRepository odontologoRepository;

    public TurnoService(TurnoRepository turnoRepository,
                        PacienteRepository pacienteRepository,
                        OdontologoRepository odontologoRepository) {
        this.turnoRepository = turnoRepository;
        this.pacienteRepository = pacienteRepository;
        this.odontologoRepository = odontologoRepository;
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

    public Turno crearTurno(TurnoRequest request) {
        validarFecha(request.fecha());
        Turno turno = construirTurno(request);
        boolean disponible = comprobarDisponibilidad(turno.getOdontologo().getId(), turno.getFecha(), turno.getHora());
        if (!disponible) {
            throw new IllegalStateException("El odontólogo no tiene disponibilidad en la fecha y hora seleccionadas.");
        }
        return turnoRepository.save(turno);
    }

    public Turno actualizarTurno(Long id, TurnoRequest request) {
        Turno turno = obtenerTurno(id);
        Turno details = construirTurno(request);
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

    private Turno construirTurno(TurnoRequest request) {
        Paciente paciente = pacienteRepository.findById(request.pacienteId())
                .orElseThrow(() -> new IllegalArgumentException("El paciente indicado no existe."));
        Odontologo odontologo = odontologoRepository.findById(request.odontologoId())
                .orElseThrow(() -> new IllegalArgumentException("El odontólogo indicado no existe."));
        String estado = request.estado() == null || request.estado().isBlank()
                ? "PENDIENTE"
                : request.estado();
        return new Turno(request.fecha(), request.hora(), request.motivo(), estado, paciente, odontologo);
    }

    private void validarFecha(LocalDate fecha) {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha del turno es obligatoria.");
        }
        if (fecha.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(FECHA_TURNO_PASADA_MESSAGE);
        }
    }
}
