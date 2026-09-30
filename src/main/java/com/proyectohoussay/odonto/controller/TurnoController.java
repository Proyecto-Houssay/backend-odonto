package com.proyectohoussay.odonto.controller;

import com.proyectohoussay.odonto.model.Turno;
import com.proyectohoussay.odonto.dto.TurnoRequest;
import com.proyectohoussay.odonto.service.TurnoService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @GetMapping
    public List<Turno> listarTurnos() {
        return turnoService.listarTurnos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerTurno(@PathVariable Long id) {
        try {
            Turno turno = turnoService.obtenerTurno(id);
            return ResponseEntity.ok(turno);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/fecha/{fecha}")
    public List<Turno> listarPorFecha(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return turnoService.listarPorFecha(fecha);
    }

    @GetMapping("/odontologo/{odontologoId}")
    public List<Turno> listarPorOdontologo(@PathVariable Long odontologoId) {
        return turnoService.listarPorOdontologo(odontologoId);
    }

    @GetMapping("/paciente/{pacienteId}")
    public List<Turno> listarPorPaciente(@PathVariable Long pacienteId) {
        return turnoService.listarPorPaciente(pacienteId);
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
            return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
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
            return ResponseEntity.ok(actualizado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelarTurno(@PathVariable Long id) {
        try {
            turnoService.cancelarTurno(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
