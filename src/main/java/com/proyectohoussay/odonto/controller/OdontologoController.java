package com.proyectohoussay.odonto.controller;

import com.proyectohoussay.odonto.dto.OdontologoAltaCompletaRequest;
import com.proyectohoussay.odonto.model.Odontologo;
import com.proyectohoussay.odonto.model.DisponibilidadHoraria;
import com.proyectohoussay.odonto.service.OdontologoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/odontologos")
@CrossOrigin(origins = "*")
@SecurityRequirement(name = "bearerAuth")
public class OdontologoController {

    private final OdontologoService odontologoService;

    public OdontologoController(OdontologoService odontologoService) {
        this.odontologoService = odontologoService;
    }

    @GetMapping
    @Operation(summary = "Listar odontólogos")
    public List<Odontologo> listarOdontologos() {
        return odontologoService.listarOdontologos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Odontologo> obtenerOdontologo(@PathVariable Long id) {
        return ResponseEntity.ok(odontologoService.obtenerOdontologo(id));
    }

    @GetMapping("/especialidad/{especialidadId}")
    @Operation(summary = "Filtrar odontólogos por especialidad")
    public List<Odontologo> listarPorEspecialidad(@PathVariable Long especialidadId) {
        return odontologoService.listarPorEspecialidad(especialidadId);
    }

    @GetMapping("/{id}/disponibilidad")
    @Operation(summary = "Consultar disponibilidad semanal de un odontólogo")
    public List<DisponibilidadHoraria> obtenerDisponibilidad(@PathVariable Long id) {
        return odontologoService.obtenerDisponibilidad(id);
    }

    @PutMapping("/{id}/disponibilidad")
    @Operation(summary = "Actualizar disponibilidad semanal de un odontólogo")
    public ResponseEntity<?> actualizarDisponibilidad(@PathVariable Long id,
                                                       @Valid @RequestBody List<DisponibilidadHoraria> disponibilidad) {
        try {
            return ResponseEntity.ok(odontologoService.actualizarDisponibilidad(id, disponibilidad));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @PutMapping("/{id}/especialidades")
    @Operation(summary = "Actualizar las especialidades de un odontólogo")
    public ResponseEntity<?> actualizarEspecialidades(@PathVariable Long id, @RequestBody List<Long> ids) {
        try {
            return ResponseEntity.ok(odontologoService.actualizarEspecialidades(id, ids));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/matricula/{matricula}")
    public ResponseEntity<Odontologo> obtenerPorMatricula(@PathVariable String matricula) {
        return odontologoService.obtenerPorMatricula(matricula)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> crearOdontologo(@Valid @RequestBody Odontologo odontologo) {
        try {
            Odontologo nuevo = odontologoService.crearOdontologo(odontologo);
            return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @PostMapping("/alta-completa")
    @Operation(summary = "Registrar odontólogo con especialidad nueva/existente y cuenta de usuario opcional")
    public ResponseEntity<?> crearOdontologoAltaCompleta(@RequestBody OdontologoAltaCompletaRequest request) {
        try {
            Odontologo nuevo = odontologoService.crearOdontologoAltaCompleta(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarOdontologo(@PathVariable Long id, @Valid @RequestBody Odontologo odontologo) {
        try {
            Odontologo actualizado = odontologoService.actualizarOdontologo(id, odontologo);
            return ResponseEntity.ok(actualizado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarOdontologo(@PathVariable Long id) {
        odontologoService.eliminarOdontologo(id);
        return ResponseEntity.noContent().build();
    }
}
