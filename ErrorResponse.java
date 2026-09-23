package com.proyectohoussay.backendodonto;

import java.time.LocalDateTime;

public record ErrorResponse(
        String mensaje,
        int codigo,
        LocalDateTime timestamp
) {
}