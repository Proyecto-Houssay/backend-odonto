package com.proyectohoussay.odonto.clinicalhistory.ai;

/**
 * Dental pathologies and clinical conditions detectable by the AI imaging module.
 */
public enum PatologiaDental {
    CARIES_DENTAL("Caries Dental", "Desmineralización y cavitación del tejido dental (esmalte, dentina o cemento)."),
    PERDIDA_OSEA_PERIODONTAL("Pérdida Ósea / Periodontitis", "Reabsorción horizontal o vertical del hueso alveolar por enfermedad periodontal."),
    LESION_PERIAPICAL("Lesión Radiolúcida Periapical", "Zona radiolúcida en el ápice radicular asociada a necrosis pulpar, granuloma o quiste."),
    DIENTE_RETENIDO_IMPACTADO("Diente Retenido / Tercer Molar Impactado", "Pieza dental que no ha erupcionado en la arcada (común en terceros molares / muelas del juicio)."),
    RESTAURACION_DEFECTUOSA("Restauración / Obturación Defectuosa", "Filtración marginal, desajuste oclusal o fractura en resina / amalgama previa."),
    FRACTURA_RADICULAR("Fractura Dental o Radicular", "Línea de discontinuidad estructural en corona o raíz dental."),
    CALCULO_DENTAL("Cálculo / Sarro Dental Subgingival", "Depósitos calcificados de placa bacteriana adheridos a superficies dentarias.");

    private final String nombreLegible;
    private final String descripcion;

    PatologiaDental(String nombreLegible, String descripcion) {
        this.nombreLegible = nombreLegible;
        this.descripcion = descripcion;
    }

    public String getNombreLegible() {
        return nombreLegible;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
