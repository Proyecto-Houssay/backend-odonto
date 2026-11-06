package com.proyectohoussay.odonto.patient;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.List;

@RestController
@SecurityRequirement(name = "bearerAuth")
public class ReporteDentalController {
    private final ReporteDentalService service;

    public ReporteDentalController(ReporteDentalService service) { this.service = service; }

    @PostMapping("/api/portal-paciente/reportes")
    public ResponseEntity<Detalle> crear(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody Nuevo request) {
        ReporteDental reporte = service.crear(PacienteCuentaController.userId(jwt), request.pieza(), request.dolor(),
                request.sintomas(), request.fotoBase64(), Boolean.TRUE.equals(request.consentimientoFoto()));
        return ResponseEntity.created(URI.create("/api/portal-paciente/reportes/" + reporte.getId()))
                .body(Detalle.de(reporte));
    }

    @GetMapping("/api/portal-paciente/reportes")
    public List<Resumen> propios(@AuthenticationPrincipal Jwt jwt) {
        return service.propios(PacienteCuentaController.userId(jwt)).stream().map(Resumen::de).toList();
    }

    @GetMapping("/api/portal-paciente/reportes/{id}")
    public Detalle propio(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return Detalle.de(service.propio(PacienteCuentaController.userId(jwt), id));
    }

    @GetMapping("/api/seguimiento/reportes")
    public List<Resumen> clinicos(@RequestParam(required = false) Long pacienteId) {
        return service.clinicos(pacienteId).stream().map(Resumen::de).toList();
    }

    @GetMapping("/api/seguimiento/reportes/{id}")
    public Detalle clinico(@PathVariable Long id) { return Detalle.de(service.clinico(id)); }

    @PutMapping("/api/seguimiento/reportes/{id}/revision")
    public Detalle revisar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt,
                           @Valid @RequestBody Revision request) {
        return Detalle.de(service.revisar(id, PacienteCuentaController.userId(jwt), request.revisionProfesional()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> invalid(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(exception.getMessage());
    }

    public record Nuevo(@NotNull Integer pieza, @NotNull @Min(0) @Max(10) Integer dolor,
                        @NotBlank @Size(max = 500) String sintomas,
                        @Size(max = 1_400_000) String fotoBase64, Boolean consentimientoFoto) { }

    public record Revision(@NotBlank @Size(max = 2000) String revisionProfesional) { }

    public record Resumen(Long id, Long pacienteId, int pieza, int dolor, String sintomas,
                          boolean fotoDisponible, Instant creadoEn, String estado,
                          String revisionProfesional, Instant revisadoEn) {
        static Resumen de(ReporteDental reporte) {
            return new Resumen(reporte.getId(), reporte.getPaciente().getId(), reporte.getPieza(),
                    reporte.getDolor(), reporte.getSintomas(), reporte.getFotoBase64() != null,
                    reporte.getCreadoEn(), reporte.getRevisadoEn() == null ? "PENDIENTE" : "REVISADO",
                    reporte.getRevisionProfesional(), reporte.getRevisadoEn());
        }
    }

    public record Detalle(Long id, Long pacienteId, int pieza, int dolor, String sintomas,
                          String fotoBase64, boolean consentimientoFoto, Instant creadoEn, String estado,
                          String revisionProfesional, Long revisadoPorId, Instant revisadoEn) {
        static Detalle de(ReporteDental reporte) {
            return new Detalle(reporte.getId(), reporte.getPaciente().getId(), reporte.getPieza(),
                    reporte.getDolor(), reporte.getSintomas(), reporte.getFotoBase64(), reporte.isConsentimientoFoto(),
                    reporte.getCreadoEn(), reporte.getRevisadoEn() == null ? "PENDIENTE" : "REVISADO",
                    reporte.getRevisionProfesional(), reporte.getRevisadoPorId(), reporte.getRevisadoEn());
        }
    }
}
