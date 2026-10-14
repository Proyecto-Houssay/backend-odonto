package com.proyectohoussay.odonto.clinicalhistory;

import com.proyectohoussay.odonto.clinicalhistory.dto.DiagnosticoDto;
import com.proyectohoussay.odonto.clinicalhistory.dto.DiagnosticoRequest;
import com.proyectohoussay.odonto.clinicalhistory.dto.TratamientoDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for managing clinical diagnoses and their associated treatments.
 */
@RestController
@RequestMapping("/api/diagnosticos")
@CrossOrigin(origins = "*")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Diagnósticos", description = "Gestión de diagnósticos clínicos y planes de tratamiento")
public class DiagnosticoController {

    private final HistoriaClinicaService historiaClinicaService;

    public DiagnosticoController(HistoriaClinicaService historiaClinicaService) {
        this.historiaClinicaService = historiaClinicaService;
    }

    @Operation(summary = "Registrar diagnóstico", description = "Crea un nuevo diagnóstico asociado a una historia clínica existente.")
    @PostMapping
    public ResponseEntity<DiagnosticoDto> crear(@Valid @RequestBody DiagnosticoRequest request) {
        Diagnostico diagnostico = historiaClinicaService.agregarDiagnostico(
                request.historiaClinicaId(),
                request.fecha(),
                request.descripcion(),
                request.tratamientos()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(DiagnosticoDto.de(diagnostico));
    }

    @Operation(summary = "Obtener diagnóstico por ID", description = "Recupera un diagnóstico odontológico junto a sus tratamientos.")
    @GetMapping("/{id}")
    public ResponseEntity<DiagnosticoDto> obtenerPorId(@PathVariable Long id) {
        Diagnostico diagnostico = historiaClinicaService.obtenerDiagnosticoPorId(id);
        return ResponseEntity.ok(DiagnosticoDto.de(diagnostico));
    }

    @Operation(summary = "Listar diagnósticos por historia clínica", description = "Obtiene todos los diagnósticos registrados para una historia clínica específica.")
    @GetMapping("/historia/{historiaClinicaId}")
    public ResponseEntity<List<DiagnosticoDto>> listarPorHistoria(@PathVariable Long historiaClinicaId) {
        List<DiagnosticoDto> diagnosticos = historiaClinicaService.listarDiagnosticosPorHistoria(historiaClinicaId).stream()
                .map(DiagnosticoDto::de)
                .toList();
        return ResponseEntity.ok(diagnosticos);
    }

    @Operation(summary = "Agregar tratamiento a diagnóstico", description = "Añade una acción terapéutica o tratamiento planificado a un diagnóstico.")
    @PostMapping("/{id}/tratamientos")
    public ResponseEntity<TratamientoDto> agregarTratamiento(
            @PathVariable Long id,
            @Valid @RequestBody TratamientoDto request
    ) {
        Tratamiento tratamiento = historiaClinicaService.agregarTratamiento(
                id,
                request.descripcion(),
                request.fecha(),
                request.estado()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(TratamientoDto.de(tratamiento));
    }

    @Operation(summary = "Actualizar estado de tratamiento", description = "Modifica el estado de avance (PENDIENTE, EN_CURSO, COMPLETADO, CANCELADO) de un tratamiento.")
    @PatchMapping("/tratamientos/{tratamientoId}/estado")
    public ResponseEntity<TratamientoDto> actualizarEstado(
            @PathVariable Long tratamientoId,
            @RequestParam EstadoTratamiento nuevoEstado
    ) {
        Tratamiento actualizado = historiaClinicaService.actualizarEstadoTratamiento(tratamientoId, nuevoEstado);
        return ResponseEntity.ok(TratamientoDto.de(actualizado));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .filter(errorMessage -> errorMessage != null && !errorMessage.isBlank())
                .findFirst()
                .orElse("Los datos del diagnóstico no son válidos.");
        return ResponseEntity.badRequest().body(message);
    }
}
