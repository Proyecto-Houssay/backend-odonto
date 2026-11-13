package com.proyectohoussay.odonto.clinicalhistory;

public class ClinicalRecordNotFoundException extends RuntimeException {
    public ClinicalRecordNotFoundException(String resource, Long id) {
        super(resource + " no encontrado con ID: " + id);
    }
}
