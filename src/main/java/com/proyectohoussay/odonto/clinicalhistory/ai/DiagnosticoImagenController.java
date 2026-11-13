package com.proyectohoussay.odonto.clinicalhistory.ai;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
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
import com.proyectohoussay.odonto.repository.OdontologoRepository;
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
@Tag(name = "Diagnóstico por Imagen con IA", description = "Recepción de estudios dentales y orientación informativa para evaluación profesional")
public class DiagnosticoImagenController {

    private final DiagnosticoImagenService diagnosticoImagenService;
    @Autowired(required = false)
    private OdontologoRepository odontologoRepository;

    public DiagnosticoImagenController(DiagnosticoImagenService diagnosticoImagenService) {
        this.diagnosticoImagenService = diagnosticoImagenService;
    }

    @Operation(summary = "Orientación inicial para ortodoncia", description = "Recibe la cobertura observada por un profesional, sugiere opciones para conversar y lista ortodoncistas activos. No interpreta radiografías ni indica un tratamiento definitivo.")
    @PostMapping("/orientacion-ortodoncia")
    public ResponseEntity<OrientacionOrtodonciaResponse> orientarOrtodoncia(@Valid @RequestBody OrientacionOrtodonciaRequest request) {
        var cobertura = request.coberturaObservada();
        String alcance = switch (cobertura) {
            case SUPERIOR -> "Arcada superior";
            case INFERIOR -> "Arcada inferior";
            case AMBAS -> "Arcadas superior e inferior";
            case INDETERMINADA -> "Cobertura por determinar en consulta";
        };
        var profesionales = (odontologoRepository == null ? List.<com.proyectohoussay.odonto.model.Odontologo>of() : odontologoRepository.findAllByActivoTrue()).stream()
                .filter(o -> o.getEspecialidad() != null && o.getEspecialidad().getNombre() != null
                        && o.getEspecialidad().getNombre().toLowerCase().contains("ortodon"))
                .map(o -> new OrientacionOrtodonciaResponse.ProfesionalOrtodoncia(
                        o.getId(), o.getNombre() + " " + o.getApellido(), o.getEmail(), o.getTelefono(), o.getEspecialidad().getNombre()))
                .toList();
        var response = new OrientacionOrtodonciaResponse(
                alcance,
                "La cobertura indicada requiere valoración clínica; una radiografía por sí sola no permite decidir si hacen falta brackets o placas ni en qué arcada.",
                List.of("Brackets fijos (metálicos o estéticos), si el ortodoncista los considera adecuados.",
                        "Placas alineadoras removibles, si el caso y la colaboración del paciente lo permiten.",
                        "Placa de contención, únicamente cuando corresponda a una indicación de retención."),
                "Odontólogo/a especialista en Ortodoncia",
                profesionales,
                "Orientación informativa, no diagnóstico ni prescripción. El tipo de aparato y la cobertura se definen con examen clínico, radiografías completas y planificación del ortodoncista."
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Analizar radiografía panorámica", description = "Envía una imagenUrl HTTPS accesible al proveedor DR. ANNIE. La integración actual admite panorámicas y devuelve el resultado bruto del proveedor para revisión profesional.")
    @PostMapping("/analizar")
    public ResponseEntity<AnalisisImagenResponse> analizar(@Valid @RequestBody AnalisisImagenRequest request) {
        AnalisisImagenResponse response = diagnosticoImagenService.analizarImagen(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Cargar archivo y analizar radiografía panorámica", description = "Con S3 privado configurado, almacena temporalmente el archivo y comparte con DR. ANNIE una URL HTTPS firmada. Si S3 no está configurado, se puede enviar una imagenUrl HTTPS accesible.")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AnalisisImagenResponse> upload(
            @RequestParam(value = "archivo", required = false) MultipartFile archivo,
            @RequestParam("tipoImagen") TipoImagenDental tipoImagen,
            @RequestParam(value = "pacienteId", required = false) Long pacienteId,
            @RequestParam(value = "historiaClinicaId", required = false) Long historiaClinicaId,
            @RequestParam(value = "observaciones", required = false) String observaciones,
            @RequestParam(value = "guardarEnHistoria", defaultValue = "false") boolean guardarEnHistoria,
            @RequestParam(value = "imagenUrl", required = false) String imagenUrl
    ) {
        AnalisisImagenResponse response = imagenUrl == null || imagenUrl.isBlank()
                ? diagnosticoImagenService.analizarArchivo(archivo, tipoImagen, pacienteId, historiaClinicaId, observaciones, guardarEnHistoria)
                : diagnosticoImagenService.analizarArchivo(archivo, tipoImagen, pacienteId, historiaClinicaId, observaciones, guardarEnHistoria, imagenUrl);
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

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleAnalysisUnavailable(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(e.getMessage());
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
