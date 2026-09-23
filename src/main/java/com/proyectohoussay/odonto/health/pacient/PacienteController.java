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

    @PostMapping
    public ResponseEntity<?> registrarPaciente(@Valid @RequestBody Paciente paciente) {
        return ResponseEntity.ok("Paciente registrado correctamente");
    }
}
