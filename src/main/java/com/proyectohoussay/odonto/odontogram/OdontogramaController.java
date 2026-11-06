package com.proyectohoussay.odonto.odontogram;

import com.proyectohoussay.odonto.patient.PacienteNoEncontradoException;
import com.proyectohoussay.odonto.auth.UserRole;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/odontograma")
@SecurityRequirement(name = "bearerAuth")
public class OdontogramaController {
    private final OdontogramaService service;
    public OdontogramaController(OdontogramaService service) { this.service = service; }

    @GetMapping("/catalogo") public OdontogramaCatalogo.Catalogo catalogo() { return OdontogramaCatalogo.actual(); }
    @PostMapping
    public ResponseEntity<OdontogramaResponse> crear(@Valid @RequestBody OdontogramaRequest request, @AuthenticationPrincipal Jwt jwt) {
        Long creator;
        try { creator = Long.valueOf(jwt.getClaimAsString("userId")); }
        catch (RuntimeException exception) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "El token no contiene un usuario válido"); }
        if (creator <= 0) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "El token no contiene un usuario válido");
        var snapshot = service.crear(request, creator);
        return ResponseEntity.created(URI.create("/api/odontograma/" + snapshot.id())).body(snapshot);
    }
    @GetMapping("/{id}")
    public OdontogramaResponse obtener(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        OdontogramaResponse response = service.obtener(id);
        return UserRole.from(jwt.getClaimAsString("role"))
                .filter(role -> role == UserRole.ADMINISTRADOR || role == UserRole.ODONTOLOGO)
                .isPresent() ? response : response.sinFirma();
    }
    @GetMapping public List<OdontogramaResumen> listar(@RequestParam Long pacienteId) { return service.listar(pacienteId); }

    @ExceptionHandler(PacienteNoEncontradoException.class)
    ResponseEntity<String> noPatient(PacienteNoEncontradoException exception) { return ResponseEntity.status(404).body(exception.getMessage()); }
    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class})
    ResponseEntity<String> invalid(Exception exception) {
        return ResponseEntity.badRequest().body(exception instanceof IllegalArgumentException ? exception.getMessage() : "El cuerpo de la solicitud no es válido");
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<String> invalidFields(MethodArgumentNotValidException exception) {
        String details = exception.getBindingResult().getAllErrors().stream()
                .map(error -> {
                    String message = error.getDefaultMessage() == null ? "valor inválido" : error.getDefaultMessage();
                    return error instanceof org.springframework.validation.FieldError fieldError
                            ? fieldError.getField() + ": " + message
                            : message;
                })
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.joining("; "));
        if (details.isBlank()) details = "Revise los campos de la solicitud";
        return ResponseEntity.badRequest().body(details);
    }
}
