package com.proyectohoussay.odonto.controller;

import com.proyectohoussay.odonto.dto.ActualizarFotoRequest;
import com.proyectohoussay.odonto.dto.ActualizarPerfilRequest;
import com.proyectohoussay.odonto.dto.EnviarMensajeRequest;
import com.proyectohoussay.odonto.dto.MensajeUsuarioDto;
import com.proyectohoussay.odonto.dto.PerfilUsuarioResponse;
import com.proyectohoussay.odonto.service.MiCuentaService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.FieldError;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mi-cuenta")
@CrossOrigin(origins = "*")
@SecurityRequirement(name = "bearerAuth")
public class MiCuentaController {

    private final MiCuentaService miCuentaService;

    public MiCuentaController(MiCuentaService miCuentaService) {
        this.miCuentaService = miCuentaService;
    }

    @GetMapping("/perfil")
    public PerfilUsuarioResponse obtenerPerfil(@AuthenticationPrincipal Jwt jwt) {
        return miCuentaService.obtenerPerfilPropio(jwt);
    }

    @PutMapping("/perfil")
    public PerfilUsuarioResponse actualizarPerfil(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ActualizarPerfilRequest request) {
        return miCuentaService.actualizarPerfilPropio(jwt, request);
    }

    @PutMapping("/foto")
    public PerfilUsuarioResponse actualizarFoto(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody(required = false) ActualizarFotoRequest request) {
        return miCuentaService.actualizarFotoPropia(jwt, request != null ? request.fotoBase64() : null);
    }

    @DeleteMapping("/foto")
    public PerfilUsuarioResponse eliminarFoto(@AuthenticationPrincipal Jwt jwt) {
        return miCuentaService.eliminarFotoPropia(jwt);
    }

    @GetMapping("/mensajes")
    public List<MensajeUsuarioDto> listarMensajes(@AuthenticationPrincipal Jwt jwt) {
        return miCuentaService.listarMensajesPropios(jwt);
    }

    @PostMapping("/mensajes")
    public ResponseEntity<MensajeUsuarioDto> enviarMensaje(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody EnviarMensajeRequest request) {
        MensajeUsuarioDto creado = miCuentaService.enviarMensaje(jwt, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/mensajes/{mensajeId}/leido")
    public MensajeUsuarioDto marcarMensajeLeido(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long mensajeId) {
        return miCuentaService.marcarMensajeLeido(jwt, mensajeId);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> manejarResponseStatus(ResponseStatusException exception) {
        String mensaje = exception.getReason() != null ? exception.getReason() : "Error en la solicitud.";
        return ResponseEntity.status(exception.getStatusCode()).body(Map.of("mensaje", mensaje));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> manejarValidacion(MethodArgumentNotValidException exception) {
        String mensaje = exception.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse("Los datos enviados no son válidos.");
        return ResponseEntity.badRequest().body(Map.of("mensaje", mensaje));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> manejarJsonInvalido(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(Map.of("mensaje", "El cuerpo de la solicitud no es válido."));
    }
}
