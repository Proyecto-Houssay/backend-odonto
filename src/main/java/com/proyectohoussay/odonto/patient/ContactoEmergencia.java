package com.proyectohoussay.odonto.patient;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Column;
import jakarta.validation.constraints.Size;

@Embeddable
public class ContactoEmergencia {
    @Size(max = 120) @Column(name = "contacto_emergencia_nombre", length = 120)
    private String nombre;
    @Size(max = 60) @Column(name = "contacto_emergencia_parentesco", length = 60)
    private String parentesco;
    @Size(max = 30) @Column(name = "contacto_emergencia_telefono", length = 30)
    private String telefono;

    public ContactoEmergencia() {}
    public ContactoEmergencia(String nombre, String parentesco, String telefono) {
        this.nombre = nombre; this.parentesco = parentesco; this.telefono = telefono;
    }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getParentesco() { return parentesco; }
    public void setParentesco(String parentesco) { this.parentesco = parentesco; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
}
