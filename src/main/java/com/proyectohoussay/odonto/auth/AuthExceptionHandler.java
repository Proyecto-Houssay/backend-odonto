package com.proyectohoussay.odonto.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.stream.Collectors;

@RestControllerAdvice(assignableTypes = AuthController.class)
public class AuthExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AuthErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining(". "));

        if (message.isBlank()) {
            message = "La solicitud contiene campos inválidos.";
        }
        return ResponseEntity.badRequest().body(new AuthErrorResponse(message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<AuthErrorResponse> handleUnreadableBody(HttpMessageNotReadableException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new AuthErrorResponse("El cuerpo de la solicitud es inválido."));
    }

    @ExceptionHandler(InvalidCurrentPasswordException.class)
    public ResponseEntity<AuthErrorResponse> handleIncorrectCurrentPassword(InvalidCurrentPasswordException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new AuthErrorResponse("La contraseña actual es incorrecta."));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<AuthErrorResponse> handleInvalidNewPassword(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new AuthErrorResponse(exception.getMessage()));
    }
}
