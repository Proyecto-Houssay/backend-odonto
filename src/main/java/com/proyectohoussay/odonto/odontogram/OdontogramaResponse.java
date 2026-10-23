package com.proyectohoussay.odonto.odontogram;

import java.time.Instant;
import java.util.List;

public record OdontogramaResponse(Long id, Long pacienteId, Instant creadoEn, Long creadoPorId, String moneda,
        String versionCatalogo, List<Diente> dientes, int totalPresupuesto, boolean consentimientoAceptado,
        String versionConsentimiento, String textoConsentimiento, String firmaBase64) {
    public record Diente(int numero, String afeccion, String nombre, String color, String descripcion, String tratamiento, int costoEstimado) {}
}
