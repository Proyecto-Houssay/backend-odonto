package com.proyectohoussay.odonto.patient;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;

@RestController
@SecurityRequirement(name = "bearerAuth")
public class PacienteCuentaController {
    private final PacienteCuentaService service;

    public PacienteCuentaController(PacienteCuentaService service) { this.service = service; }

    @GetMapping("/api/portal-paciente/perfil")
    public PacienteResponse propio(@AuthenticationPrincipal Jwt jwt) {
        return service.propio(userId(jwt));
    }

    @PostMapping("/api/portal-paciente/perfil")
    public ResponseEntity<PacienteResponse> crear(@AuthenticationPrincipal Jwt jwt,
                                                  @Valid @RequestBody PerfilRequest request) {
        PacienteResponse response = service.crear(userId(jwt), request.dni(), request.telefono(), request.fechaNacimiento());
        return ResponseEntity.created(URI.create("/api/portal-paciente/perfil")).body(response);
    }

    @PutMapping("/api/portal-paciente/perfil")
    public PacienteResponse actualizarTelefono(@AuthenticationPrincipal Jwt jwt,
                                               @Valid @RequestBody ContactoRequest request) {
        return service.actualizarTelefono(userId(jwt), request.telefono());
    }

    @PutMapping("/api/admin/vinculos-paciente/{userId}")
    public PacienteResponse vincular(@PathVariable Long userId, @Valid @RequestBody VinculoRequest request) {
        return service.vincular(userId, request.pacienteId(), request.dniConfirmado());
    }

    static Long userId(Jwt jwt) {
        try {
            long id = Long.parseLong(jwt.getClaimAsString("userId"));
            if (id > 0) return id;
        } catch (RuntimeException ignored) { }
        throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token sin usuario válido");
    }

    public record PerfilRequest(
            @NotBlank @Size(min = 5, max = 30) String dni,
            @Size(max = 30) String telefono,
            @NotNull @PastOrPresent LocalDate fechaNacimiento) { }

    public record VinculoRequest(@NotNull @Positive Long pacienteId, @NotBlank String dniConfirmado) { }

    public record ContactoRequest(@Size(max = 30) String telefono) { }
}
