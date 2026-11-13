package com.proyectohoussay.odonto.clinicalhistory;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@SecurityRequirement(name = "bearerAuth")
public class ClinicalRecordController {
    private final ClinicalRecordService service;

    public ClinicalRecordController(ClinicalRecordService service) {
        this.service = service;
    }

    @PostMapping("/api/historias-clinicas")
    public ResponseEntity<HistoriaResponse> createHistory(@Valid @RequestBody HistoriaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(HistoriaResponse.de(
                service.createHistory(request.pacienteId(), request.fechaApertura(), request.observaciones())));
    }

    @GetMapping("/api/historias-clinicas")
    public List<HistoriaResponse> listHistories(@RequestParam(required = false) Long pacienteId) {
        return service.listHistories(pacienteId).stream().map(HistoriaResponse::de).toList();
    }

    @GetMapping("/api/historias-clinicas/{id}")
    public HistoriaResponse getHistory(@PathVariable Long id) {
        return HistoriaResponse.de(service.getHistory(id));
    }

    @PutMapping("/api/historias-clinicas/{id}")
    public HistoriaResponse updateHistory(@PathVariable Long id, @Valid @RequestBody HistoriaUpdateRequest request) {
        return HistoriaResponse.de(service.updateHistory(id, request.fechaApertura(), request.observaciones()));
    }

    @PostMapping("/api/diagnosticos")
    public ResponseEntity<DiagnosticoResponse> createDiagnosis(@Valid @RequestBody DiagnosticoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(DiagnosticoResponse.de(
                service.createDiagnosis(request.historiaClinicaId(), request.fecha(), request.descripcion())));
    }

    @GetMapping("/api/diagnosticos")
    public List<DiagnosticoResponse> listDiagnoses(@RequestParam(required = false) Long historiaClinicaId) {
        return service.listDiagnoses(historiaClinicaId).stream().map(DiagnosticoResponse::de).toList();
    }

    @GetMapping("/api/diagnosticos/{id}")
    public DiagnosticoResponse getDiagnosis(@PathVariable Long id) {
        return DiagnosticoResponse.de(service.getDiagnosis(id));
    }

    @PutMapping("/api/diagnosticos/{id}")
    public DiagnosticoResponse updateDiagnosis(@PathVariable Long id, @Valid @RequestBody DiagnosticoUpdateRequest request) {
        return DiagnosticoResponse.de(service.updateDiagnosis(id, request.fecha(), request.descripcion()));
    }

    @PostMapping("/api/tratamientos")
    public ResponseEntity<TratamientoResponse> createTreatment(@Valid @RequestBody TratamientoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(TratamientoResponse.de(
                service.createTreatment(request.diagnosticoId(), request.descripcion(), request.fecha(), request.estado())));
    }

    @GetMapping("/api/tratamientos")
    public List<TratamientoResponse> listTreatments(@RequestParam(required = false) Long diagnosticoId) {
        return service.listTreatments(diagnosticoId).stream().map(TratamientoResponse::de).toList();
    }

    @GetMapping("/api/tratamientos/{id}")
    public TratamientoResponse getTreatment(@PathVariable Long id) {
        return TratamientoResponse.de(service.getTreatment(id));
    }

    @PutMapping("/api/tratamientos/{id}")
    public TratamientoResponse updateTreatment(@PathVariable Long id, @Valid @RequestBody TratamientoUpdateRequest request) {
        return TratamientoResponse.de(service.updateTreatment(id, request.descripcion(), request.fecha(), request.estado()));
    }

    @ExceptionHandler(ClinicalRecordNotFoundException.class)
    public ResponseEntity<String> notFound(ClinicalRecordNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }

    @ExceptionHandler(ClinicalRecordConflictException.class)
    public ResponseEntity<String> conflict(ClinicalRecordConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> invalidFields(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .filter(value -> value != null && !value.isBlank())
                .findFirst().orElse("Los datos clínicos no son válidos");
        return ResponseEntity.badRequest().body(message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> invalidJson(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body("El cuerpo de la solicitud no es válido");
    }
}
