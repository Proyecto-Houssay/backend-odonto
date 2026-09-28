package com.proyectohoussay.odonto.patient;

import org.springframework.stereotype.Service;

@Service
public class PacienteService {

    private static final String DUPLICATE_DNI_MESSAGE = "Ya existe un paciente registrado con ese DNI";

    private final PacienteRepository pacienteRepository;

    public PacienteService(PacienteRepository pacienteRepository) {
        this.pacienteRepository = pacienteRepository;
    }

    public Paciente registrarPaciente(Paciente paciente) {
        if (pacienteRepository.existsByDni(paciente.getDni())) {
            throw new PacienteDuplicadoException(DUPLICATE_DNI_MESSAGE);
        }

        return pacienteRepository.save(paciente);
    }
}
