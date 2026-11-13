package com.proyectohoussay.odonto.model;

/**
 * Roles and job positions for administrative and auxiliary clinic staff.
 */
public enum CargoPersonal {
    RECEPCIONISTA("Recepcionista", "Atención al paciente, agendamiento de turnos y recepción."),
    ASISTENTE_DENTAL("Asistente Dental", "Soporte instrumental y asistencia clínica al odontólogo."),
    SECRETARIO("Secretario/a", "Gestión administrativa, archivo y facturación básica."),
    ADMINISTRATIVO("Personal Administrativo", "Supervisión contable, insumos y trámites generales."),
    MANTENIMIENTO("Mantenimiento", "Higiene, desinfección y mantenimiento de consultorios."),
    LABORATORIO("Técnico de Laboratorio", "Prótesis dentales, vaciados y confección de aparatos.");

    private final String nombreLegible;
    private final String descripcion;

    CargoPersonal(String nombreLegible, String descripcion) {
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
