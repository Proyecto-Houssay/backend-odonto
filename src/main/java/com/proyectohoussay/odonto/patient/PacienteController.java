package com.proyectohoussay.odonto.patient;

import com.proyectohoussay.odonto.dto.PacienteCompletoDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/buscar")
    @Operation(summary = "Buscar pacientes por nombre o apellido")
    public List<PacienteCompletoDto> buscar(@RequestParam String termino) {
        return pacienteService.buscar(termino).stream().map(PacienteCompletoDto::de).toList();
    }

    @GetMapping("/dni/{dni}")
    @Operation(summary = "Buscar paciente por DNI")
    public ResponseEntity<?> obtenerPorDni(@PathVariable String dni) {
        try {
            return ResponseEntity.ok(PacienteCompletoDto.de(pacienteService.obtenerPorDni(dni)));
        } catch (IllegalArgumentException | PacienteNoEncontradoException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}")
    public PacienteResponse obtenerPaciente(@PathVariable Long id) {
        return PacienteResponse.de(pacienteService.obtenerPaciente(id));
    }

    @PutMapping("/{id}")
    public PacienteResponse actualizarPaciente(@PathVariable Long id, @Valid @RequestBody PacienteRequest request) {
        return PacienteResponse.de(pacienteService.actualizarPaciente(id, request.toEntity()));
    }

    @PutMapping("/{id}/cobertura")
    @Operation(summary = "Actualizar cobertura médica del paciente")
    public ResponseEntity<?> actualizarCobertura(@PathVariable Long id, @RequestBody CoberturaRequest request) {
        try {
            return ResponseEntity.ok(PacienteCompletoDto.de(
                    pacienteService.actualizarCobertura(
                            id,
                            request.obraSocialId(),
                            request.numeroAfiliado(),
                            request.plan(),
                            request.particular()
                    )));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/alertas")
    @Operation(summary = "Actualizar alertas médicas del paciente")
    public ResponseEntity<?> actualizarAlertas(@PathVariable Long id, @RequestBody List<String> alertas) {
        try {
            return ResponseEntity.ok(PacienteCompletoDto.de(
                    pacienteService.actualizarAlertasMedicas(id, alertas)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
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
