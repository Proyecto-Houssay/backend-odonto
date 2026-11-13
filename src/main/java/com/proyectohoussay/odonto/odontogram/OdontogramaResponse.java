package com.proyectohoussay.odonto.odontogram;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

public record OdontogramaResponse(Long id, Long pacienteId, Instant creadoEn, Long creadoPorId, String moneda,
        String versionCatalogo, List<Diente> dientes, int totalPresupuesto, boolean consentimientoAceptado,
        String versionConsentimiento, String textoConsentimiento,
        @JsonInclude(JsonInclude.Include.NON_NULL) String firmaBase64) {
    public OdontogramaResponse sinFirma() {
        return new OdontogramaResponse(id, pacienteId, creadoEn, creadoPorId, moneda, versionCatalogo,
                dientes, totalPresupuesto, consentimientoAceptado, versionConsentimiento, textoConsentimiento, null);
    }

    public record Diente(int numero, String afeccion, String nombre, String color, String descripcion, String tratamiento, int costoEstimado) {}
}
