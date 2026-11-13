package com.proyectohoussay.odonto.ai;

import com.google.ai.generativeai.GenerativeModel;
import com.google.ai.generativeai.Part;
import com.google.ai.generativeai.response.GenerateContentResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ai")
public class RadiografiaController {

    // Precios de brackets y placas (ejemplo estático; en producción podría ser un llamado a otro servicio)
    private static final java.util.Map<String, Double> PRECIOS = java.util.Map.of(
            "Bracket estándar", 12.5,
            "Bracket premium", 22.0,
            "Placa de zirconio", 45.0,
            "Placa de porcelana", 38.0
    );

    private final GenerativeModel model;

    public RadiografiaController() {
        // API key is taken from environment variable GEMINI_API_KEY
        this.model = new GenerativeModel.Builder()
                .modelName("gemini-1.5-flash")
                .apiKey(System.getenv("GEMINI_API_KEY"))
                .build();
    }

    @PostMapping(value = "/analizar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> analizar(@RequestParam("file") MultipartFile file) throws Exception {
        // Convert file to Part for Gemini Vision
        Part imagePart = Part.fromMimeType(file.getContentType(), file.getBytes());

        String prompt = """
                Eres la Dra. Ziegler, experta en odontología.
                Analiza la radiografía que recibes y devuelve:
                1️⃣ Una lista de hallazgos (p.ej. caries, pérdida ósea, implantes).
                2️⃣ Recomendaciones de diagnóstico y tratamiento.
                Usa un lenguaje claro y conciso.
                """;

        GenerateContentResponse response = model.generateContent(
                Part.text(prompt),
                imagePart
        );

        String answer = response.getText();
        String preciosFormateados = PRECIOS.entrySet()
        .stream()
        .map(e -> String.format("%s: $%.2f", e.getKey(), e.getValue()))
        .collect(java.util.stream.Collectors.joining("\n"));

        String respuestaConPrecios = answer + "\n\n--- Precios actuales de brackets y placas ---\n" + preciosFormateados;
        return ResponseEntity.ok(respuestaConPrecios);
    }
}
