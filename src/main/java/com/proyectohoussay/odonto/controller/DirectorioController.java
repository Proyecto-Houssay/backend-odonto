package com.proyectohoussay.odonto.controller;

import com.proyectohoussay.odonto.dto.DirectorioItemDto;
import com.proyectohoussay.odonto.service.DirectorioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/directorio")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Directorio", description = "Búsqueda de personal, profesionales y pacientes")
public class DirectorioController {
    private final DirectorioService directorioService;
    public DirectorioController(DirectorioService directorioService) { this.directorioService = directorioService; }

    @GetMapping("/buscar")
    @Operation(summary = "Buscar en el directorio institucional")
    public ResponseEntity<?> buscar(
            @RequestParam(required = false) String termino,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(required = false) String tipo) {
        try {
            String effectiveQuery = (termino != null && !termino.isBlank()) ? termino : q;
            List<DirectorioItemDto> items = (effectiveQuery == null || effectiveQuery.isBlank())
                    ? directorioService.listar(tipo)
                    : directorioService.buscar(effectiveQuery, tipo);
            return ResponseEntity.ok(items);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
