package com.proyectohoussay.odonto.odontogram;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.zip.CRC32;

/** Bounds and verifies PNG structure before allocating a decoded raster. */
final class FirmaPng {
    private static final String PREFIX = "data:image/png;base64,";
    private static final byte[] MAGIC = {(byte)137, 80, 78, 71, 13, 10, 26, 10};
    private FirmaPng() {}

    static void validar(String dataUrl) {
        if (dataUrl == null || !dataUrl.startsWith(PREFIX) || dataUrl.length() > 349550) invalid();
        final byte[] bytes;
        try { bytes = Base64.getDecoder().decode(dataUrl.substring(PREFIX.length())); }
        catch (IllegalArgumentException exception) { throw new IllegalArgumentException("La firma debe ser un PNG Base64 válido"); }
        if (bytes.length > 262144 || bytes.length < 57 || !Arrays.equals(MAGIC, Arrays.copyOf(bytes, 8))) invalid();
        var buffer = ByteBuffer.wrap(bytes);
        if (buffer.getInt(8) != 13 || !"IHDR".equals(new String(bytes, 12, 4, StandardCharsets.US_ASCII))) invalid();
        int width = buffer.getInt(16), height = buffer.getInt(20);
        if (width < 1 || height < 1 || width > 2048 || height > 2048 || (long)width * height > 1000000) {
            throw new IllegalArgumentException("La firma excede las dimensiones permitidas (2048 por lado, 1 millón de píxeles)");
        }
        int offset = 8;
        boolean ended = false;
        while (offset < bytes.length) {
            if (bytes.length - offset < 12) invalid();
            int length = buffer.getInt(offset);
            if (length < 0 || length > bytes.length - offset - 12) invalid();
            var crc = new CRC32(); crc.update(bytes, offset + 4, length + 4);
            if (crc.getValue() != Integer.toUnsignedLong(buffer.getInt(offset + 8 + length))) invalid();
            String type = new String(bytes, offset + 4, 4, StandardCharsets.US_ASCII);
            offset += length + 12;
            if ("IEND".equals(type)) {
                if (length != 0 || offset != bytes.length) invalid();
                ended = true; break;
            }
        }
        if (!ended) invalid();
        try {
            var image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null || image.getWidth() != width || image.getHeight() != height) invalid();
        } catch (IOException | RuntimeException exception) {
            throw new IllegalArgumentException("La firma contiene un PNG corrupto o no válido");
        }
    }
    private static void invalid() { throw new IllegalArgumentException("La firma debe ser un PNG válido de hasta 256 KiB"); }
}
