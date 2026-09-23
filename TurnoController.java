package com.proyectohoussay.backendodonto;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/turnos")
@RequiredArgsConstructor
public class TurnoController {

    private final TurnoService turnoService;

    @PostMapping
    public ResponseEntity<TurnoResponse> registrar(@Valid @RequestBody TurnoRequest request) {
        TurnoResponse response = turnoService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}