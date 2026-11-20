package com.proyectohoussay.odonto.odontogram;

import java.time.Instant;
import java.util.List;

public record PacienteOdontogramaResponse(Instant registradoEn, List<Pieza> piezas) {
    public record Pieza(int numero, String afeccion, String tratamiento) {}
}
