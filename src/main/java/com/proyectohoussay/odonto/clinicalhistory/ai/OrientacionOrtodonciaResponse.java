package com.proyectohoussay.odonto.clinicalhistory.ai;

import java.util.List;

public record OrientacionOrtodonciaResponse(
        String alcance,
        String recomendacion,
        List<String> alternativasAConversar,
        String profesionalRecomendado,
        List<ProfesionalOrtodoncia> profesionalesDisponibles,
        String aviso
) {
    public record ProfesionalOrtodoncia(
            Long id,
            String nombre,
            String email,
            String telefono,
            String especialidad
    ) {
    }
}
