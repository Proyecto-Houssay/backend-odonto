package com.proyectohoussay.odonto.controller;

import com.proyectohoussay.odonto.model.Turno;
import com.proyectohoussay.odonto.dto.TurnoRequest;
import com.proyectohoussay.odonto.dto.TurnoResponse;
import com.proyectohoussay.odonto.exception.TurnoNoEncontradoException;
import com.proyectohoussay.odonto.service.TurnoService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/turnos")
@CrossOrigin(origins = "*")
public class TurnoController {

    private final TurnoService turnoService;

    public TurnoController(TurnoService turnoService) {
        this.turnoService = turnoService;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .filter(errorMessage -> errorMessage != null && !errorMessage.isBlank())
                .findFirst()
                .orElse("Los datos del turno no son válidos");

        return ResponseEntity.badRequest().body(message);
    }

    @GetMapping
    public List<TurnoResponse> listarTurnos() {
        return turnoService.listarTurnos().stream()
                .map(turno -> TurnoResponse.de(turno, null))
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerTurno(@PathVariable Long id) {
        try {
            Turno turno = turnoService.obtenerTurno(id);
            return ResponseEntity.ok(TurnoResponse.de(turno, null));
        } catch (TurnoNoEncontradoException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/fecha/{fecha}")
    public List<TurnoResponse> listarPorFecha(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return turnoService.listarPorFecha(fecha).stream()
                .map(turno -> TurnoResponse.de(turno, null))
                .toList();
    }

    @GetMapping("/odontologo/{odontologoId}")
    public List<TurnoResponse> listarPorOdontologo(@PathVariable Long odontologoId) {
        return turnoService.listarPorOdontologo(odontologoId).stream()
                .map(turno -> TurnoResponse.de(turno, null))
                .toList();
    }

    @GetMapping("/paciente/{pacienteId}")
    public List<TurnoResponse> listarPorPaciente(@PathVariable Long pacienteId) {
        return turnoService.listarPorPaciente(pacienteId).stream()
                .map(turno -> TurnoResponse.de(turno, null))
                .toList();
    }

    @GetMapping("/disponibilidad")
    public ResponseEntity<Boolean> comprobarDisponibilidad(
            @RequestParam Long odontologoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime hora) {
        boolean disponible = turnoService.comprobarDisponibilidad(odontologoId, fecha, hora);
        return ResponseEntity.ok(disponible);
    }

    @PostMapping
    public ResponseEntity<?> crearTurno(@Valid @RequestBody TurnoRequest turno) {
        try {
            Turno nuevo = turnoService.crearTurno(turno);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(TurnoResponse.de(nuevo, "Turno registrado con éxito"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarTurno(@PathVariable Long id, @Valid @RequestBody TurnoRequest turno) {
        try {
            Turno actualizado = turnoService.actualizarTurno(id, turno);
            return ResponseEntity.ok(TurnoResponse.de(actualizado, "Turno actualizado correctamente"));
        } catch (TurnoNoEncontradoException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelarTurno(@PathVariable Long id) {
        try {
            turnoService.cancelarTurno(id);
            return ResponseEntity.noContent().build();
        } catch (TurnoNoEncontradoException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
