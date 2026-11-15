package com.proyectohoussay.odonto.chatbot;

import com.proyectohoussay.odonto.chatbot.dto.DoctorDisponibleDto;
import com.proyectohoussay.odonto.chatbot.dto.ReservaChatbotRequest;
import com.proyectohoussay.odonto.chatbot.dto.ReservaChatbotResponse;
import com.proyectohoussay.odonto.chatbot.dto.TurnoSlotDto;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Endpoints del asistente para consultar doctores disponibles, horarios libres reales
 * y reservar turnos que quedan persistidos en la agenda del consultorio.
 */
@RestController
@RequestMapping("/api/chatbot")
@CrossOrigin(origins = "*")
@SecurityRequirement(name = "bearerAuth")
public class ChatbotAgendaController {

    private final ChatbotAgendaService chatbotAgendaService;

    public ChatbotAgendaController(ChatbotAgendaService chatbotAgendaService) {
        this.chatbotAgendaService = chatbotAgendaService;
    }

    @GetMapping("/doctores")
    public List<DoctorDisponibleDto> listarDoctoresDisponibles() {
        return chatbotAgendaService.listarDoctoresDisponibles();
    }

    @GetMapping("/turnos/disponibles")
    public List<TurnoSlotDto> listarTurnosDisponibles(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) Long odontologoId) {
        int dias = fecha != null ? 1 : 7;
        int limite = fecha != null ? 48 : 20;
        return chatbotAgendaService.buscarTurnosDisponibles(odontologoId, fecha, dias, limite);
    }

    @PostMapping("/turnos/reservar")
    public ResponseEntity<ReservaChatbotResponse> reservarTurno(
            @Valid @RequestBody ReservaChatbotRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Long userId = extraerUserId(jwt);
        String role = jwt != null ? jwt.getClaimAsString("role") : null;
        ReservaChatbotResponse creada = chatbotAgendaService.reservarDesdeChat(request, userId, role);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(exception.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleConflict(IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .filter(text -> text != null && !text.isBlank())
                .findFirst()
                .orElse("Los datos para reservar el turno no son válidos.");
        return ResponseEntity.badRequest().body(message);
    }

    private static Long extraerUserId(Jwt jwt) {
        if (jwt == null) {
            return null;
        }
        try {
            String claim = jwt.getClaimAsString("userId");
            return claim != null ? Long.valueOf(claim) : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
