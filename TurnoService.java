package com.proyectohoussay.backendodonto;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional
public class TurnoService {

    private final TurnoRepository turnoRepository;
    private final PacienteRepository pacienteRepository;
    private final OdontologoRepository odontologoRepository;

    public TurnoResponse registrar(TurnoRequest request) {

        Paciente paciente = pacienteRepository.findById(request.pacienteId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Paciente no encontrado con id " + request.pacienteId()));

        Odontologo odontologo = odontologoRepository.findById(request.odontologoId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Odontólogo no encontrado con id " + request.odontologoId()));

        if (request.fecha().isBefore(LocalDate.now())) {
            throw new FechaInvalidaException(
                    "No se permiten turnos en fechas anteriores a la actual");
        }

        boolean horarioOcupado = turnoRepository.existsByOdontologoIdAndFechaAndHorarioAndEstadoNot(
                request.odontologoId(),
                request.fecha(),
                request.horario(),
                EstadoTurno.CANCELADO
        );

        if (horarioOcupado) {
            throw new TurnoNoDisponibleException(
                    "El horario seleccionado ya está ocupado para el odontólogo");
        }

        Turno turno = new Turno();
        turno.setPaciente(paciente);
        turno.setOdontologo(odontologo);
        turno.setFecha(request.fecha());
        turno.setHorario(request.horario());
        turno.setMotivo(request.motivo());
        turno.setEstado(EstadoTurno.REGISTRADO);

        Turno guardado = turnoRepository.save(turno);

        return toResponse(guardado);
    }

    private TurnoResponse toResponse(Turno turno) {
        return new TurnoResponse(
                turno.getId(),
                turno.getPaciente().getId(),
                turno.getPaciente().getNombre() + " " + turno.getPaciente().getApellido(),
                turno.getOdontologo().getId(),
                turno.getOdontologo().getNombre() + " " + turno.getOdontologo().getApellido(),
                turno.getFecha(),
                turno.getHorario(),
                turno.getMotivo(),
                turno.getEstado().name(),
                "Turno registrado exitosamente"
        );
    }
}