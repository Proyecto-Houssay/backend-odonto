package com.proyectohoussay.odonto.clinicalhistory;

import com.proyectohoussay.odonto.clinicalhistory.dto.DiagnosticoDto;
import com.proyectohoussay.odonto.clinicalhistory.dto.DiagnosticoRequest;
import com.proyectohoussay.odonto.clinicalhistory.dto.HistoriaClinicaRequest;
import com.proyectohoussay.odonto.clinicalhistory.dto.HistoriaClinicaResponse;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for patient clinical histories.
 */
@RestController
@RequestMapping("/api/historias-clinicas")
@CrossOrigin(origins = "*")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Historias Clínicas", description = "Gestión de fichas odontológicas e historias clínicas")
public class HistoriaClinicaController {

    private final HistoriaClinicaService historiaClinicaService;

    public HistoriaClinicaController(HistoriaClinicaService historiaClinicaService) {
        this.historiaClinicaService = historiaClinicaService;
    }

    @Operation(summary = "Crear historia clínica", description = "Abre una nueva historia clínica asociada a un paciente.")
    @PostMapping
    public ResponseEntity<HistoriaClinicaResponse> crear(@Valid @RequestBody HistoriaClinicaRequest request) {
        HistoriaClinica creada = historiaClinicaService.crearHistoriaClinica(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(HistoriaClinicaResponse.de(creada));
    }

    @Operation(summary = "Obtener historia clínica por ID", description = "Recupera la historia clínica completa con sus diagnósticos y tratamientos.")
    @GetMapping("/{id}")
    public ResponseEntity<HistoriaClinicaResponse> obtenerPorId(@PathVariable Long id) {
        HistoriaClinica historia = historiaClinicaService.obtenerPorId(id);
        return ResponseEntity.ok(HistoriaClinicaResponse.de(historia));
    }

    @Operation(summary = "Obtener historia clínica por ID de paciente", description = "Recupera la historia clínica del paciente indicado.")
    @GetMapping("/paciente/{pacienteId}")
    public ResponseEntity<HistoriaClinicaResponse> obtenerPorPacienteId(@PathVariable Long pacienteId) {
        HistoriaClinica historia = historiaClinicaService.obtenerPorPacienteId(pacienteId);
        return ResponseEntity.ok(HistoriaClinicaResponse.de(historia));
    }

    @Operation(summary = "Agregar diagnóstico a historia clínica", description = "Registra un nuevo diagnóstico odontológico y sus tratamientos asociados en la historia clínica.")
    @PostMapping("/{id}/diagnosticos")
    public ResponseEntity<DiagnosticoDto> agregarDiagnostico(
            @PathVariable Long id,
            @Valid @RequestBody DiagnosticoRequest request
    ) {
        Diagnostico diagnostico = historiaClinicaService.agregarDiagnostico(
                id,
                request.fecha(),
                request.descripcion(),
                request.tratamientos()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(DiagnosticoDto.de(diagnostico));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .filter(errorMessage -> errorMessage != null && !errorMessage.isBlank())
                .findFirst()
                .orElse("Los datos de la historia clínica no son válidos.");
        return ResponseEntity.badRequest().body(message);
    }
}
