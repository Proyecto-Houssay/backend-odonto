package com.proyectohoussay.odonto.patient;

import com.proyectohoussay.odonto.odontogram.PacienteOdontogramaResponse;
import com.proyectohoussay.odonto.odontogram.PacienteOdontogramaService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portal-paciente")
@SecurityRequirement(name = "bearerAuth")
public class PacienteOdontogramaController {
    private final PacienteOdontogramaService service;

    public PacienteOdontogramaController(PacienteOdontogramaService service) { this.service = service; }

    @GetMapping("/odontograma")
    public PacienteOdontogramaResponse odontograma(@AuthenticationPrincipal Jwt jwt) {
        return service.propio(PacienteCuentaController.userId(jwt));
    }
}
