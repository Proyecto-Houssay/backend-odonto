package com.proyectohoussay.odonto.odontogram;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class OdontogramaDiente {
    @Column(nullable = false) private int numero;
    @Column(nullable = false) private String afeccion;
    @Column(nullable = false) private String nombre;
    @Column(nullable = false) private String color;
    @Column(nullable = false) private String descripcion;
    @Column(nullable = false) private String tratamiento;
    @Column(nullable = false) private int costoEstimado;
    protected OdontogramaDiente() {}
    OdontogramaDiente(int numero, OdontogramaCatalogo.Afeccion item) {
        this.numero = numero; this.afeccion = item.afeccion(); this.nombre = item.nombre(); this.color = item.color();
        this.descripcion = item.descripcion(); this.tratamiento = item.tratamiento(); this.costoEstimado = item.costoEstimado();
    }
    OdontogramaResponse.Diente response() {
        return new OdontogramaResponse.Diente(numero, afeccion, nombre, color, descripcion, tratamiento, costoEstimado);
    }
    int costo() { return costoEstimado; }
}
