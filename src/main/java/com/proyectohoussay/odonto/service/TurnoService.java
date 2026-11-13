package com.proyectohoussay.odonto.service;

import com.proyectohoussay.odonto.model.Turno;
import com.proyectohoussay.odonto.dto.TurnoRequest;
import com.proyectohoussay.odonto.dto.DisponibilidadAgendaResponse;
import com.proyectohoussay.odonto.exception.TurnoNoEncontradoException;
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

    private static final String ESTADO_CANCELADO = "CANCELADO";

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
                .orElseThrow(() -> new TurnoNoEncontradoException(id));
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
        return !turnoRepository.existeTurnoActivoEnHorario(odontologoId, fecha, hora, null);
    }

    public DisponibilidadAgendaResponse listarHorariosDisponibles(Long odontologoId, LocalDate fecha) {
        Odontologo odontologo = odontologoRepository.findById(odontologoId)
                .orElseThrow(() -> new IllegalArgumentException("El odontólogo indicado no existe."));

        if (!odontologo.isActivo() || fecha.isBefore(LocalDate.now())) {
            return new DisponibilidadAgendaResponse(odontologoId, fecha, List.of());
        }

        // Horario institucional vigente: lunes-viernes 08:00-20:00, sábados 09:00-14:00.
        // No hay franjas individuales persistidas por profesional; la reserva vuelve a validar el slot.
        LocalTime inicio;
        LocalTime fin;
        switch (fecha.getDayOfWeek()) {
            case SATURDAY -> { inicio = LocalTime.of(9, 0); fin = LocalTime.of(14, 0); }
            case SUNDAY -> { return new DisponibilidadAgendaResponse(odontologoId, fecha, List.of()); }
            default -> { inicio = LocalTime.of(8, 0); fin = LocalTime.of(20, 0); }
        }

        List<LocalTime> ocupados = turnoRepository.findByFechaAndOdontologoId(fecha, odontologoId).stream()
                .filter(turno -> !estaCancelado(turno.getEstado()))
                .map(Turno::getHora)
                .toList();

        LocalTime primerHorario = inicio;
        if (fecha.equals(LocalDate.now())) {
            LocalTime proximaFranja = LocalTime.now().withSecond(0).withNano(0);
            int minutos = proximaFranja.getMinute();
            proximaFranja = proximaFranja.withMinute(minutos < 30 ? 30 : 0);
            if (minutos >= 30) proximaFranja = proximaFranja.plusHours(1);
            if (proximaFranja.isAfter(primerHorario)) primerHorario = proximaFranja;
        }

        List<LocalTime> disponibles = new java.util.ArrayList<>();
        for (LocalTime slot = primerHorario; slot.isBefore(fin); slot = slot.plusMinutes(30)) {
            LocalTime slotInicio = slot;
            LocalTime slotFin = slot.plusMinutes(30);
            boolean solapado = ocupados.stream().anyMatch(ocupado ->
                    ocupado.isBefore(slotFin) && ocupado.plusMinutes(30).isAfter(slotInicio));
            if (slotFin.compareTo(fin) <= 0 && !solapado) disponibles.add(slot);
        }
        return new DisponibilidadAgendaResponse(odontologoId, fecha, List.copyOf(disponibles));
    }

    public Turno crearTurno(TurnoRequest request) {
        validarFechaNoPasada(request.fecha());
        Turno turno = construirTurno(request);
        if (!estaCancelado(turno.getEstado()) && turnoRepository.existeTurnoActivoEnHorario(
                turno.getOdontologo().getId(), turno.getFecha(), turno.getHora(), null)) {
            throw new IllegalStateException("El odontólogo no tiene disponibilidad en la fecha y hora seleccionadas.");
        }
        return turnoRepository.save(turno);
    }

    public Turno actualizarTurno(Long id, TurnoRequest request) {
        Turno turno = obtenerTurno(id);
        validarFechaNoPasada(request.fecha());
        Turno details = construirTurno(request);
        if (!estaCancelado(details.getEstado()) && turnoRepository.existeTurnoActivoEnHorario(
                details.getOdontologo().getId(), details.getFecha(), details.getHora(), id)) {
            throw new IllegalStateException("El odontólogo no tiene disponibilidad en la fecha y hora seleccionadas.");
        }
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
        turno.setEstado(ESTADO_CANCELADO);
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

    private void validarFechaNoPasada(LocalDate fecha) {
        if (fecha != null && fecha.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("No se permiten turnos en fechas anteriores a la actual.");
        }
    }

    private boolean estaCancelado(String estado) {
        return estado != null && ESTADO_CANCELADO.equalsIgnoreCase(estado);
    }
}
