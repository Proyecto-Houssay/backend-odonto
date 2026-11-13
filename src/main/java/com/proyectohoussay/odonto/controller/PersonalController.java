package com.proyectohoussay.odonto.controller;

import com.proyectohoussay.odonto.dto.PersonalRequest;
import com.proyectohoussay.odonto.dto.PersonalResponse;
import com.proyectohoussay.odonto.model.CargoPersonal;
import com.proyectohoussay.odonto.model.Personal;
import com.proyectohoussay.odonto.service.PersonalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.CrossOrigin;
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
import java.util.Map;

@RestController
@RequestMapping("/api/personal")
@CrossOrigin(origins = "*")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Personal", description = "Gestión del personal administrativo, auxiliar y operativo de la clínica")
public class PersonalController {

    private final PersonalService personalService;

    public PersonalController(PersonalService personalService) {
        this.personalService = personalService;
    }

    @Operation(summary = "Registrar nuevo personal", description = "Crea un registro de personal administrativo o auxiliar con DNI único y cargo.")
    @PostMapping
    public ResponseEntity<PersonalResponse> crear(@Valid @RequestBody PersonalRequest request) {
        Personal personal = personalService.crearPersonal(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(PersonalResponse.de(personal));
    }

    @Operation(summary = "Listar personal", description = "Devuelve el listado de personal con filtros opcionales por cargo y estado activo.")
    @GetMapping
    public ResponseEntity<List<PersonalResponse>> listar(
            @RequestParam(required = false) CargoPersonal cargo,
            @RequestParam(required = false) Boolean activo
    ) {
        List<PersonalResponse> list = personalService.listarPersonal(cargo, activo).stream()
                .map(PersonalResponse::de)
                .toList();
        return ResponseEntity.ok(list);
    }

    @Operation(summary = "Obtener personal por ID", description = "Recupera la ficha de un miembro del personal por su identificador único.")
    @GetMapping("/{id}")
    public ResponseEntity<PersonalResponse> obtenerPorId(@PathVariable Long id) {
        Personal personal = personalService.obtenerPorId(id);
        return ResponseEntity.ok(PersonalResponse.de(personal));
    }

    @Operation(summary = "Obtener personal por DNI", description = "Busca a un integrante del personal mediante su número de documento.")
    @GetMapping("/dni/{dni}")
    public ResponseEntity<PersonalResponse> obtenerPorDni(@PathVariable String dni) {
        Personal personal = personalService.obtenerPorDni(dni);
        return ResponseEntity.ok(PersonalResponse.de(personal));
    }

    @Operation(summary = "Actualizar datos del personal", description = "Modifica los datos personales, cargo, horario laboral o vinculación de usuario.")
    @PutMapping("/{id}")
    public ResponseEntity<PersonalResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody PersonalRequest request
    ) {
        Personal actualizado = personalService.actualizarPersonal(id, request);
        return ResponseEntity.ok(PersonalResponse.de(actualizado));
    }

    @Operation(summary = "Dar de baja lógica al personal", description = "Cambia el estado del personal a inactivo preservando el histórico.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> darDeBaja(@PathVariable Long id) {
        personalService.darDeBaja(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("mensaje", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensaje", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException exception) {
        String mensaje = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .filter(msg -> msg != null && !msg.isBlank())
                .findFirst()
                .orElse("Los datos del personal no son válidos.");
        return ResponseEntity.badRequest().body(Map.of("mensaje", mensaje));
    }
}
