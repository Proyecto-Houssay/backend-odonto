package com.proyectohoussay.odonto.controller;

import com.proyectohoussay.odonto.model.Odontologo;
import com.proyectohoussay.odonto.service.OdontologoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/odontologos")
@CrossOrigin(origins = "*")
public class OdontologoController {

    private final OdontologoService odontologoService;

    public OdontologoController(OdontologoService odontologoService) {
        this.odontologoService = odontologoService;
    }

    @GetMapping
    public List<Odontologo> listarOdontologos() {
        return odontologoService.listarOdontologos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Odontologo> obtenerOdontologo(@PathVariable Long id) {
        return ResponseEntity.ok(odontologoService.obtenerOdontologo(id));
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
