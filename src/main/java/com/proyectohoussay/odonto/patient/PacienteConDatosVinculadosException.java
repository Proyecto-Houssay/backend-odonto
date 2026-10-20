package com.proyectohoussay.odonto.patient;

public class PacienteConDatosVinculadosException extends IllegalStateException {
    public PacienteConDatosVinculadosException() {
        super("No se puede eliminar un paciente con historia clínica, turnos u odontogramas");
    }
}
