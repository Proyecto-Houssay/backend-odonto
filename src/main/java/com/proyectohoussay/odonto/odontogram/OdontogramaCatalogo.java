package com.proyectohoussay.odonto.odontogram;

import java.util.List;

public final class OdontogramaCatalogo {
    public static final String VERSION = "academic-v1";
    public static final String CONSENTIMIENTO = "Comprendo que este odontograma y presupuesto son una simulación académica. "
            + "Las afecciones seleccionadas, los tratamientos y los precios son ilustrativos y requieren validación profesional. "
            + "He revisado los dientes marcados, sus tratamientos y el total estimado en ARS. "
            + "Acepto registrar este presupuesto ilustrativo con mi firma manuscrita electrónica. "
            + "No constituye un diagnóstico, una indicación de tratamiento ni un comprobante de pago.";
    public record Afeccion(String afeccion, String nombre, String color, String descripcion, String tratamiento, int costoEstimado) {}
    public record Catalogo(String versionCatalogo, String versionConsentimiento, String textoConsentimiento, List<Afeccion> afecciones) {}
    public static final List<Afeccion> AFECCIONES = List.of(
            new Afeccion("caries", "Caries", "#ef4444", "Destrucción del esmalte por bacterias", "Limpieza + empaste con resina compuesta", 45000),
            new Afeccion("fractura", "Fractura", "#f97316", "Rotura por golpe", "Reconstrucción con corona o carilla", 85000),
            new Afeccion("gingivitis", "Gingivitis", "#eab308", "Inflamación de encía por placa", "Limpieza ultrasónica + antiséptico", 30000),
            new Afeccion("ausente", "Ausente", "#6b7280", "Diente perdido", "Implante dental o prótesis puente", 250000),
            new Afeccion("sano", "Sano", "#22c55e", "Sin afección visible", "Control cada 6 meses", 0));

    private OdontogramaCatalogo() {}
    public static Catalogo actual() { return new Catalogo(VERSION, VERSION, CONSENTIMIENTO, AFECCIONES); }
    public static Afeccion buscar(String code) {
        return AFECCIONES.stream().filter(item -> item.afeccion().equals(code)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("La afección no pertenece al catálogo vigente"));
    }
}
