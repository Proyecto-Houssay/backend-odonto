package com.proyectohoussay.odonto.controller;

import com.proyectohoussay.odonto.model.Especialidad;
import com.proyectohoussay.odonto.service.EspecialidadService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/especialidades")
@CrossOrigin(origins = "*")
public class EspecialidadController {

    private final EspecialidadService especialidadService;

    public EspecialidadController(EspecialidadService especialidadService) {
        this.especialidadService = especialidadService;
    }

    @GetMapping
    public List<Especialidad> listarEspecialidades() {
        return especialidadService.listarEspecialidades();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Especialidad> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(especialidadService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<Especialidad> crearEspecialidad(@Valid @RequestBody Especialidad especialidad) {
        Especialidad creada = especialidadService.crearEspecialidad(especialidad);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarEspecialidad(@PathVariable Long id) {
        especialidadService.eliminarEspecialidad(id);
        return ResponseEntity.noContent().build();
    }
}
