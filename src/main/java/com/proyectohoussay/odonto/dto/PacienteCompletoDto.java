package com.proyectohoussay.odonto.dto;

import com.proyectohoussay.odonto.patient.ContactoEmergencia;
import com.proyectohoussay.odonto.patient.Paciente;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PacienteCompletoDto(
        Long id, String nombre, String apellido, String dni, String telefono, String email,
        LocalDate fechaNacimiento, Long obraSocialId, String obraSocial, String numeroAfiliado,
        String planCobertura, boolean particular, ContactoEmergencia contactoEmergencia,
        List<String> alertasMedicas, LocalDateTime creadoEn, LocalDateTime actualizadoEn
) {
    public static PacienteCompletoDto de(Paciente paciente) {
        return new PacienteCompletoDto(paciente.getId(), paciente.getNombre(), paciente.getApellido(), paciente.getDni(),
                paciente.getTelefono(), paciente.getEmail(), paciente.getFechaNacimiento(),
                paciente.getObraSocial() == null ? null : paciente.getObraSocial().getId(),
                paciente.getObraSocial() == null ? null : paciente.getObraSocial().getNombre(),
                paciente.getNumeroAfiliado(), paciente.getPlanCobertura(), paciente.isParticular(),
                paciente.getContactoEmergencia(), List.copyOf(paciente.getAlertasMedicas()),
                paciente.getCreadoEn(), paciente.getActualizadoEn());
    }
}
