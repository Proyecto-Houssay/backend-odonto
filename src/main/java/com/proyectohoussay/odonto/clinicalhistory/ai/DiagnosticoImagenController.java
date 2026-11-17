package com.proyectohoussay.odonto.clinicalhistory.ai;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST controller for Dental Image Diagnosis with AI.
 * Enables automated detection of caries, periodontal bone loss, periapical lesions,
 * impacted molars, and defective restorations from dental radiographs and photographs.
 */
@RestController
@RequestMapping("/api/ia/diagnostico-imagen")
@CrossOrigin(origins = "*")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Diagnóstico por Imagen con IA", description = "Análisis automatizado de radiografías y fotografías dentales asistido por IA")
public class DiagnosticoImagenController {

    private final DiagnosticoImagenService diagnosticoImagenService;

    public DiagnosticoImagenController(DiagnosticoImagenService diagnosticoImagenService) {
        this.diagnosticoImagenService = diagnosticoImagenService;
    }

    @Operation(summary = "Analizar imagen dental en formato Base64", description = "Procesa una radiografía o foto dental en Base64, detecta patologías con IA y sugiere tratamientos.")
    @PostMapping("/analizar")
    public ResponseEntity<AnalisisImagenResponse> analizar(@Valid @RequestBody AnalisisImagenRequest request) {
        AnalisisImagenResponse response = diagnosticoImagenService.analizarImagen(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Subir y analizar archivo de imagen dental (Multipart)", description = "Sube un archivo de radiografía o fotografía dental y ejecuta el análisis de diagnóstico asistido por IA.")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AnalisisImagenResponse> upload(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam("tipoImagen") TipoImagenDental tipoImagen,
            @RequestParam(value = "pacienteId", required = false) Long pacienteId,
            @RequestParam(value = "historiaClinicaId", required = false) Long historiaClinicaId,
            @RequestParam(value = "observaciones", required = false) String observaciones,
            @RequestParam(value = "guardarEnHistoria", defaultValue = "false") boolean guardarEnHistoria
    ) {
        AnalisisImagenResponse response = diagnosticoImagenService.analizarArchivo(
                archivo,
                tipoImagen,
                pacienteId,
                historiaClinicaId,
                observaciones,
                guardarEnHistoria
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Obtener historial de análisis de IA por paciente", description = "Recupera la lista cronológica de estudios diagnósticos por IA realizados a un paciente.")
    @GetMapping("/paciente/{pacienteId}")
    public ResponseEntity<List<DiagnosticoImagen>> obtenerPorPaciente(@PathVariable Long pacienteId) {
        List<DiagnosticoImagen> historial = diagnosticoImagenService.obtenerHistorialPorPaciente(pacienteId);
        return ResponseEntity.ok(historial);
    }

    @Operation(summary = "Obtener detalle de un diagnóstico por imagen por ID", description = "Devuelve el resultado completo y los hallazgos de un estudio específico.")
    @GetMapping("/{id}")
    public ResponseEntity<DiagnosticoImagen> obtenerPorId(@PathVariable Long id) {
        DiagnosticoImagen diagnostico = diagnosticoImagenService.obtenerPorId(id);
        return ResponseEntity.ok(diagnostico);
    }

    @Operation(summary = "Catálogo de capacidades del módulo de IA", description = "Devuelve los tipos de imágenes dentales, patologías detectables y niveles de severidad soportados.")
    @GetMapping("/catalogo")
    public ResponseEntity<CatalogoIaDto> obtenerCatalogo() {
        CatalogoIaDto catalogo = diagnosticoImagenService.obtenerCatalogo();
        return ResponseEntity.ok(catalogo);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .filter(errorMessage -> errorMessage != null && !errorMessage.isBlank())
                .findFirst()
                .orElse("Los datos de la solicitud de diagnóstico por imagen no son válidos.");
        return ResponseEntity.badRequest().body(message);
    }
}
