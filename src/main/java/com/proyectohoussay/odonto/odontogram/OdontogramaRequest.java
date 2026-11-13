package com.proyectohoussay.odonto.odontogram;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record OdontogramaRequest(
        @NotNull @Positive Long pacienteId,
        @NotNull @Size(min = 1, max = 32, message = "Debe marcar entre 1 y 32 dientes") List<@NotNull @Valid Diente> dientes,
        @NotNull @AssertTrue(message = "Debe aceptar el consentimiento") Boolean consentimientoAceptado,
        @NotBlank String versionConsentimiento,
        @NotBlank @Size(max = 349550, message = "La firma supera el tamaño permitido") String firmaBase64) {
    public record Diente(@NotNull Integer numero, @NotBlank String afeccion) {}
}
