package com.proyectohoussay.odonto.patient;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Iterator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class FotoClinica {
    private static final Pattern DATA_URL = Pattern.compile("^data:image/(png|jpeg);base64,([A-Za-z0-9+/=]+)$");
    private static final int MAX_INPUT_BYTES = 1_000_000;
    private static final int MAX_OUTPUT_BYTES = 2_000_000;
    private static final int MAX_DIMENSION = 2048;

    private FotoClinica() { }

    static String normalizar(String dataUrl) {
        if (dataUrl == null || dataUrl.isBlank()) return null;
        if (dataUrl.length() > 1_400_000) throw new IllegalArgumentException("La foto supera el tamaño permitido");
        Matcher match = DATA_URL.matcher(dataUrl);
        if (!match.matches()) throw new IllegalArgumentException("La foto debe ser PNG o JPEG");
        try {
            byte[] bytes = Base64.getDecoder().decode(match.group(2));
            if (bytes.length == 0 || bytes.length > MAX_INPUT_BYTES) {
                throw new IllegalArgumentException("La foto supera el tamaño permitido");
            }
            try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw new IllegalArgumentException("La foto no contiene una imagen válida");
                ImageReader reader = readers.next();
                try {
                    reader.setInput(input, true, true);
                    String actual = reader.getFormatName().toLowerCase(Locale.ROOT);
                    if (!actual.equals(match.group(1))) throw new IllegalArgumentException("El formato de la foto no coincide");
                    int width = reader.getWidth(0), height = reader.getHeight(0);
                    if (width <= 0 || height <= 0 || width > MAX_DIMENSION || height > MAX_DIMENSION) {
                        throw new IllegalArgumentException("La foto debe medir hasta 2048 píxeles por lado");
                    }
                    BufferedImage image = reader.read(0);
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    if (!ImageIO.write(image, "png", output) || output.size() > MAX_OUTPUT_BYTES) {
                        throw new IllegalArgumentException("La foto procesada supera el tamaño permitido");
                    }
                    return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
                } finally {
                    reader.dispose();
                }
            }
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new IllegalArgumentException("No se pudo leer la foto", exception);
        }
    }
}
