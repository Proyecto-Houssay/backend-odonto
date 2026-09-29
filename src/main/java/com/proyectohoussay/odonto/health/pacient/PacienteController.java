package com.proyectohoussay.odonto.patient;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pacientes")
public class PacienteController {

    private final PacienteRepository pacienteRepository;

    public PacienteController(PacienteRepository pacienteRepository) {
        this.pacienteRepository = pacienteRepository;
    }

    @PostMapping
    public ResponseEntity<?> registrarPaciente(@Valid @RequestBody Paciente paciente) {

        if (pacienteRepository.existsByDni(paciente.getDni())) {
            return ResponseEntity.status(409)
                    .body("Ya existe un paciente registrado con ese DNI");
        }

        pacienteRepository.save(paciente);

        return ResponseEntity.ok("Paciente registrado correctamente");
    }
}
