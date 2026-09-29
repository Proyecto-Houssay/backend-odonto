package com.proyectohoussay.odonto.controller;

import com.proyectohoussay.odonto.model.Turno;
import com.proyectohoussay.odonto.service.TurnoService;
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
    public ResponseEntity<Turno> obtenerTurno(@PathVariable Long id) {
        return ResponseEntity.ok(turnoService.obtenerTurno(id));
    }

    @GetMapping("/fecha/{fecha}")
    public List<Turno> listarPorFecha(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return turnoService.listarPorFecha(fecha);
    }

    @GetMapping("/odontologo/{odontologoId}")
    public List<Turno> listarPorOdontologo(@PathVariable Long odontologoId) {
        return turnoService.listarPorOdontologo(odontologoId);
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
    public ResponseEntity<?> crearTurno(@RequestBody Turno turno) {
        try {
            Turno nuevo = turnoService.crearTurno(turno);
            return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Turno> actualizarTurno(@PathVariable Long id, @RequestBody Turno turno) {
        return ResponseEntity.ok(turnoService.actualizarTurno(id, turno));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelarTurno(@PathVariable Long id) {
        turnoService.cancelarTurno(id);
        return ResponseEntity.noContent().build();
    }
}
