package com.proyectohoussay.odonto.clinicalhistory.ai;

import java.util.List;

/**
 * Metadata and catalog of supported AI image analysis features.
 */
public record CatalogoIaDto(
        List<TipoImagenInfo> tiposImagen,
        List<PatologiaInfo> patologias,
        List<UrgenciaInfo> nivelesUrgencia,
        String versionModelo,
        String estadoServicio
) {
    public record TipoImagenInfo(TipoImagenDental tipo, String nombre, String descripcion) {
        public static TipoImagenInfo de(TipoImagenDental tipo) {
            return new TipoImagenInfo(tipo, tipo.getNombreLegible(), tipo.getDescripcion());
        }
    }

    public record PatologiaInfo(PatologiaDental patologia, String nombre, String descripcion) {
        public static PatologiaInfo de(PatologiaDental patologia) {
            return new PatologiaInfo(patologia, patologia.getNombreLegible(), patologia.getDescripcion());
        }
    }

    public record UrgenciaInfo(NivelUrgencia nivel, String etiqueta, String descripcion) {
        public static UrgenciaInfo de(NivelUrgencia nivel) {
            return new UrgenciaInfo(nivel, nivel.getEtiqueta(), nivel.getDescripcion());
        }
    }
}
