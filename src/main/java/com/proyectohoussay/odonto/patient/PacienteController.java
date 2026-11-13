package com.proyectohoussay.odonto.patient;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import com.proyectohoussay.odonto.dto.PacienteCompletoDto;

@RestController
@RequestMapping("/api/pacientes")
@SecurityRequirement(name = "bearerAuth")
public class PacienteController {

    private final PacienteService pacienteService;

    public PacienteController(PacienteService pacienteService) {
        this.pacienteService = pacienteService;
    }

    @PostMapping
    @Operation(summary = "Registrar paciente")
    public ResponseEntity<String> registrarPaciente(@Valid @RequestBody Paciente paciente) {
        try {
            pacienteService.registrarPaciente(paciente);
            return ResponseEntity.ok("Paciente registrado correctamente");
        } catch (PacienteDuplicadoException exception) {
            return ResponseEntity.status(409)
                    .body(exception.getMessage());
        }
    }

    @GetMapping
    @Operation(summary = "Listar pacientes")
    public List<PacienteCompletoDto> listarPacientes() {
        return pacienteService.listarPacientes().stream().map(PacienteCompletoDto::de).toList();
    }

    @GetMapping("/buscar")
    @Operation(summary = "Buscar pacientes por nombre o apellido")
    public List<PacienteCompletoDto> buscar(@RequestParam String termino) {
        return pacienteService.buscar(termino).stream().map(PacienteCompletoDto::de).toList();
    }

    @GetMapping("/dni/{dni}")
    @Operation(summary = "Buscar paciente por DNI")
    public ResponseEntity<?> obtenerPorDni(@PathVariable String dni) {
        try { return ResponseEntity.ok(PacienteCompletoDto.de(pacienteService.obtenerPorDni(dni))); }
        catch (IllegalArgumentException e) { return ResponseEntity.notFound().build(); }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener ficha completa de paciente")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        try { return ResponseEntity.ok(PacienteCompletoDto.de(pacienteService.obtenerPorId(id))); }
        catch (IllegalArgumentException e) { return ResponseEntity.notFound().build(); }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar datos demográficos y clínicos del paciente")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @Valid @RequestBody Paciente paciente) {
        try { return ResponseEntity.ok(PacienteCompletoDto.de(pacienteService.actualizarPaciente(id, paciente))); }
        catch (PacienteDuplicadoException e) { return ResponseEntity.status(409).body(e.getMessage()); }
        catch (IllegalArgumentException e) { return ResponseEntity.notFound().build(); }
    }

    @PutMapping("/{id}/cobertura")
    @Operation(summary = "Actualizar cobertura médica del paciente")
    public ResponseEntity<?> actualizarCobertura(@PathVariable Long id, @RequestBody CoberturaRequest request) {
        try { return ResponseEntity.ok(PacienteCompletoDto.de(pacienteService.actualizarCobertura(id, request.obraSocialId(),
                request.numeroAfiliado(), request.plan(), request.particular()))); }
        catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @PutMapping("/{id}/alertas")
    @Operation(summary = "Actualizar alertas médicas del paciente")
    public ResponseEntity<?> actualizarAlertas(@PathVariable Long id, @RequestBody List<String> alertas) {
        try { return ResponseEntity.ok(PacienteCompletoDto.de(pacienteService.actualizarAlertasMedicas(id, alertas))); }
        catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(e.getMessage()); }
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
