package com.proyectohoussay.odonto.teletriaje.controller;

import com.proyectohoussay.odonto.patient.PacienteNoEncontradoException;
import com.proyectohoussay.odonto.teletriaje.dto.*;
import com.proyectohoussay.odonto.teletriaje.exception.*;
import com.proyectohoussay.odonto.teletriaje.model.CitaTeletriaje;
import com.proyectohoussay.odonto.teletriaje.model.SolicitudTeletriaje;
import com.proyectohoussay.odonto.teletriaje.service.TeletriajeService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/teletriaje")
@SecurityRequirement(name = "bearerAuth")
public class TeletriajeController {

    private final TeletriajeService teletriajeService;

    public TeletriajeController(TeletriajeService teletriajeService) {
        this.teletriajeService = teletriajeService;
    }

    @PostMapping("/solicitudes")
    public ResponseEntity<SolicitudTeletriajeResponse> crearSolicitud(
            @Valid @RequestBody SolicitudTeletriajeRequest request) {
        SolicitudTeletriaje guardada = teletriajeService.crearSolicitud(request);
        return ResponseEntity.created(URI.create("/api/teletriaje/solicitudes/" + guardada.getId()))
                .body(SolicitudTeletriajeResponse.de(guardada));
    }

    @GetMapping("/solicitudes")
    public List<SolicitudTeletriajeResponse> listarColaPrioridad() {
        return teletriajeService.obtenerColaPrioridad().stream()
                .map(SolicitudTeletriajeResponse::de)
                .toList();
    }

    @GetMapping("/solicitudes/{id}")
    public SolicitudTeletriajeResponse obtenerSolicitud(@PathVariable Long id) {
        return SolicitudTeletriajeResponse.de(teletriajeService.obtenerSolicitud(id));
    }

    @GetMapping("/solicitudes/paciente/{pacienteId}")
    public List<SolicitudTeletriajeResponse> listarSolicitudesPorPaciente(@PathVariable Long pacienteId) {
        return teletriajeService.listarSolicitudesPaciente(pacienteId).stream()
                .map(SolicitudTeletriajeResponse::de)
                .toList();
    }

    @PutMapping("/solicitudes/{id}/prioridad")
    public ResponseEntity<SolicitudTeletriajeResponse> validarPrioridad(
            @PathVariable Long id,
            @Valid @RequestBody ValidarPrioridadRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Long profesionalId = extraerUserId(jwt);
        String profesionalNombre = jwt != null ? jwt.getSubject() : "Profesional";

        SolicitudTeletriaje actualizada = teletriajeService.validarPrioridad(
                id, request, profesionalId, profesionalNombre);
        return ResponseEntity.ok(SolicitudTeletriajeResponse.de(actualizada));
    }

    @PostMapping("/solicitudes/{id}/reservar")
    public ResponseEntity<CitaTeletriajeResponse> reservarTurno(
            @PathVariable Long id,
            @Valid @RequestBody ReservarTeletriajeRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Long currentUserId = extraerUserId(jwt);
        String currentRole = extraerRol(jwt);

        CitaTeletriaje cita = teletriajeService.reservarTurno(id, request, currentUserId, currentRole);
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/api/teletriaje/citas/" + cita.getId()))
                .body(CitaTeletriajeResponse.de(cita));
    }

    @GetMapping("/citas/{id}")
    public CitaTeletriajeResponse obtenerCita(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        Long currentUserId = extraerUserId(jwt);
        String currentRole = extraerRol(jwt);
        String currentUsername = jwt != null ? jwt.getSubject() : null;

        CitaTeletriaje cita = teletriajeService.consultarAccesoCita(id, currentUserId, currentRole, currentUsername);
        return CitaTeletriajeResponse.de(cita);
    }

    @GetMapping("/citas/paciente/{pacienteId}")
    public List<CitaTeletriajeResponse> listarCitasPorPaciente(@PathVariable Long pacienteId) {
        return teletriajeService.listarCitasPaciente(pacienteId).stream()
                .map(CitaTeletriajeResponse::de)
                .toList();
    }

    @GetMapping("/citas/agenda")
    public ResponseEntity<?> listarAgendaTeletriaje(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long odontologoId) {
        try {
            List<AgendaTeletriajeResponse> citas = teletriajeService.listarAgenda(desde, hasta, odontologoId)
                    .stream().map(AgendaTeletriajeResponse::de).toList();
            return ResponseEntity.ok(citas);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(exception.getMessage());
        }
    }

    @PostMapping("/citas/{id}/revocar")
    public ResponseEntity<CitaTeletriajeResponse> revocarEnlace(@PathVariable Long id) {
        CitaTeletriaje revocada = teletriajeService.revocarEnlace(id);
        return ResponseEntity.ok(CitaTeletriajeResponse.de(revocada));
    }

    @ExceptionHandler(ConflictoReservaException.class)
    public ResponseEntity<String> handleConflicto(ConflictoReservaException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(exception.getMessage());
    }

    @ExceptionHandler(AccesoDenegadoTeletriajeException.class)
    public ResponseEntity<String> handleAccesoDenegado(AccesoDenegadoTeletriajeException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(exception.getMessage());
    }

    @ExceptionHandler(EnlaceExpiradoException.class)
    public ResponseEntity<String> handleEnlaceExpirado(EnlaceExpiradoException exception) {
        return ResponseEntity.status(HttpStatus.GONE).body(exception.getMessage());
    }

    @ExceptionHandler({SolicitudTeletriajeNoEncontradaException.class, CitaTeletriajeNoEncontradaException.class, PacienteNoEncontradoException.class})
    public ResponseEntity<String> handleNotFound(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<String> handleBadRequest(Exception exception) {
        return ResponseEntity.badRequest().body(exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidation(MethodArgumentNotValidException exception) {
        String mensaje = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .filter(m -> m != null && !m.isBlank())
                .findFirst()
                .orElse("Los datos de teletriaje no son válidos");
        return ResponseEntity.badRequest().body(mensaje);
    }

    private Long extraerUserId(Jwt jwt) {
        if (jwt == null) return null;
        try {
            String claim = jwt.getClaimAsString("userId");
            return claim != null ? Long.valueOf(claim) : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String extraerRol(Jwt jwt) {
        if (jwt == null) return null;
        return jwt.getClaimAsString("role");
    }
}
