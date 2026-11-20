package com.proyectohoussay.odonto.patient;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/portal-paciente")
@SecurityRequirement(name = "bearerAuth")
public class PacienteActividadController {
    private final PacienteActividadService service;

    public PacienteActividadController(PacienteActividadService service) { this.service = service; }

    @GetMapping("/turnos")
    public List<TurnoPropioResponse> turnos(@AuthenticationPrincipal Jwt jwt) {
        return service.turnosPropios(PacienteCuentaController.userId(jwt));
    }

    @GetMapping("/tratamientos")
    public List<TratamientoPropioResponse> tratamientos(@AuthenticationPrincipal Jwt jwt) {
        return service.tratamientosPropios(PacienteCuentaController.userId(jwt));
    }
}
