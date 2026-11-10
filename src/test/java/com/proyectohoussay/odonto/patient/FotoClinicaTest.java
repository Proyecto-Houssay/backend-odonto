package com.proyectohoussay.odonto.patient;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FotoClinicaTest {
    @Test
    void acceptsRealImageAndNormalizesToPng() throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        String submitted = "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());

        String normalized = FotoClinica.normalizar(submitted);

        assertThat(normalized).startsWith("data:image/png;base64,");
        assertThat(FotoClinica.normalizar(null)).isNull();
    }

    @Test
    void rejectsSvgDisguisedDataAndOversizedDimensions() throws Exception {
        assertThatThrownBy(() -> FotoClinica.normalizar("data:image/svg+xml;base64,PHN2Zz4="))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FotoClinica.normalizar("data:image/png;base64,PHN2Zz4="))
                .isInstanceOf(IllegalArgumentException.class);
        BufferedImage large = new BufferedImage(2049, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(large, "png", output);
        String submitted = "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        assertThatThrownBy(() -> FotoClinica.normalizar(submitted)).isInstanceOf(IllegalArgumentException.class);
    }
}
