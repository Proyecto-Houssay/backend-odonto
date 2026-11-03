package com.proyectohoussay.odonto.report;

import com.proyectohoussay.odonto.auth.RequireRoles;
import com.proyectohoussay.odonto.dto.MovimientoFinancieroDto;
import com.proyectohoussay.odonto.dto.MovimientoFinancieroRequest;
import com.proyectohoussay.odonto.service.MovimientoFinancieroService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/finanzas/movimientos")
@CrossOrigin(origins = "*")
public class MovimientoFinancieroController {
    private final MovimientoFinancieroService movimientoService;

    public MovimientoFinancieroController(MovimientoFinancieroService movimientoService) {
        this.movimientoService = movimientoService;
    }

    @PostMapping
    @RequireRoles("ADMIN")
    public ResponseEntity<MovimientoFinancieroDto> registrar(@Valid @RequestBody MovimientoFinancieroRequest request) {
        return ResponseEntity.status(201).body(movimientoService.registrar(request));
    }
}
