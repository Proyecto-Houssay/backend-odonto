package com.proyectohoussay.odonto.odontogram;

import com.proyectohoussay.odonto.patient.PacienteNoEncontradoException;
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
import java.util.List;

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
    @GetMapping("/{id}") public OdontogramaResponse obtener(@PathVariable Long id) { return service.obtener(id); }
    @GetMapping public List<OdontogramaResumen> listar(@RequestParam Long pacienteId) { return service.listar(pacienteId); }

    @ExceptionHandler(PacienteNoEncontradoException.class)
    ResponseEntity<String> noPatient(PacienteNoEncontradoException exception) { return ResponseEntity.status(404).body(exception.getMessage()); }
    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class})
    ResponseEntity<String> invalid(Exception exception) {
        return ResponseEntity.badRequest().body(exception instanceof IllegalArgumentException ? exception.getMessage() : "El cuerpo de la solicitud no es válido");
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<String> invalidFields(MethodArgumentNotValidException exception) {
        return ResponseEntity.badRequest().body("Revise los campos obligatorios, los dientes y el consentimiento de la solicitud");
    }
}
