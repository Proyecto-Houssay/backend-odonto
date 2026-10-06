package com.proyectohoussay.odonto.dto;

import com.proyectohoussay.odonto.model.Especialidad;
import com.proyectohoussay.odonto.model.Odontologo;
import com.proyectohoussay.odonto.model.Turno;
import com.proyectohoussay.odonto.patient.Paciente;

import java.time.LocalDate;
import java.time.LocalTime;

public record TurnoResponse(
        String mensaje,
        Long id,
        LocalDate fecha,
        LocalTime hora,
        String motivo,
        String estado,
        PacienteResumen paciente,
        OdontologoResumen odontologo) {

    public static TurnoResponse de(Turno turno, String mensaje) {
        return new TurnoResponse(
                mensaje,
                turno.getId(),
                turno.getFecha(),
                turno.getHora(),
                turno.getMotivo(),
                turno.getEstado(),
                PacienteResumen.de(turno.getPaciente()),
                OdontologoResumen.de(turno.getOdontologo())
        );
    }

    public record PacienteResumen(Long id, String nombre, String apellido, String dni,
                                  String telefono, String email, LocalDate fechaNacimiento) {
        private static PacienteResumen de(Paciente paciente) {
            if (paciente == null) return null;
            return new PacienteResumen(paciente.getId(), paciente.getNombre(), paciente.getApellido(),
                    paciente.getDni(), paciente.getTelefono(), paciente.getEmail(), paciente.getFechaNacimiento());
        }
    }

    public record OdontologoResumen(Long id, String nombre, String apellido, String matricula,
                                    String email, String telefono, boolean activo,
                                    EspecialidadResumen especialidad) {
        private static OdontologoResumen de(Odontologo odontologo) {
            if (odontologo == null) return null;
            return new OdontologoResumen(odontologo.getId(), odontologo.getNombre(), odontologo.getApellido(),
                    odontologo.getMatricula(), odontologo.getEmail(), odontologo.getTelefono(),
                    odontologo.isActivo(), EspecialidadResumen.de(odontologo.getEspecialidad()));
        }
    }

    public record EspecialidadResumen(Long id, String nombre, String descripcion) {
        private static EspecialidadResumen de(Especialidad especialidad) {
            if (especialidad == null) return null;
            return new EspecialidadResumen(especialidad.getId(), especialidad.getNombre(), especialidad.getDescripcion());
        }
    }
}
