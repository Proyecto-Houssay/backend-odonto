package com.proyectohoussay.odonto.patient;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.converter.HttpMessageNotReadableException;
import java.util.List;

@RestController
@RequestMapping("/api/pacientes")
@SecurityRequirement(name = "bearerAuth")
public class PacienteController {

    private final PacienteService pacienteService;

    public PacienteController(PacienteService pacienteService) {
        this.pacienteService = pacienteService;
    }

    @PostMapping
    public ResponseEntity<String> registrarPaciente(@Valid @RequestBody PacienteRequest paciente) {
        try {
            pacienteService.registrarPaciente(paciente.toEntity());
            return ResponseEntity.ok("Paciente registrado correctamente");
        } catch (PacienteDuplicadoException exception) {
            return ResponseEntity.status(409)
                    .body(exception.getMessage());
        }
    }

    @GetMapping
    public List<PacienteResponse> listarPacientes() {
        return pacienteService.listarPacientes().stream().map(PacienteResponse::de).toList();
    }

    @GetMapping("/{id}")
    public PacienteResponse obtenerPaciente(@PathVariable Long id) {
        return PacienteResponse.de(pacienteService.obtenerPaciente(id));
    }

    @PutMapping("/{id}")
    public PacienteResponse actualizarPaciente(@PathVariable Long id, @Valid @RequestBody PacienteRequest request) {
        return PacienteResponse.de(pacienteService.actualizarPaciente(id, request.toEntity()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarPaciente(@PathVariable Long id) {
        pacienteService.eliminarPaciente(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(PacienteNoEncontradoException.class)
    public ResponseEntity<String> handleNotFound(PacienteNoEncontradoException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }

    @ExceptionHandler({PacienteDuplicadoException.class, PacienteConDatosVinculadosException.class})
    public ResponseEntity<String> handleConflict(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(exception.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> handleUnreadable(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body("El cuerpo de la solicitud no es válido");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .filter(errorMessage -> errorMessage != null && !errorMessage.isBlank())
                .findFirst()
                .orElse("Los datos del paciente no son válidos");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
    }
}
