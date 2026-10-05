package com.proyectohoussay.odonto.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "insumos")
public class Insumo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre del insumo es obligatorio.")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres.")
    @Column(nullable = false, length = 100)
    private String nombre;

    @NotBlank(message = "La categoría del insumo es obligatoria.")
    @Size(max = 50, message = "La categoría no puede superar los 50 caracteres.")
    @Column(nullable = false, length = 50)
    private String categoria;

    @Min(value = 0, message = "La cantidad disponible no puede ser negativa.")
    @Column(nullable = false)
    private int cantidadDisponible;

    @NotBlank(message = "La unidad de medida es obligatoria.")
    @Size(max = 20, message = "La unidad de medida no puede superar los 20 caracteres.")
    @Column(nullable = false, length = 20)
    private String unidadMedida;

    @Min(value = 0, message = "El stock mínimo no puede ser negativo.")
    @Column(nullable = false)
    private int stockMinimo;

    @Column(nullable = false, length = 30)
    private String estado;

    public Insumo() {
    }

    public Insumo(String nombre, String categoria, int cantidadDisponible, String unidadMedida, int stockMinimo) {
        this.nombre = nombre;
        this.categoria = categoria;
        this.cantidadDisponible = cantidadDisponible;
        this.unidadMedida = unidadMedida;
        this.stockMinimo = stockMinimo;
        this.estado = calcularEstado(cantidadDisponible, stockMinimo);
    }

    public static String calcularEstado(int cantidad, int stockMinimo) {
        if (cantidad <= 0) {
            return "AGOTADO";
        } else if (cantidad <= stockMinimo) {
            return "BAJO_STOCK";
        } else {
            return "DISPONIBLE";
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public int getCantidadDisponible() {
        return cantidadDisponible;
    }

    public void setCantidadDisponible(int cantidadDisponible) {
        this.cantidadDisponible = cantidadDisponible;
        this.estado = calcularEstado(cantidadDisponible, this.stockMinimo);
    }

    public String getUnidadMedida() {
        return unidadMedida;
    }

    public void setUnidadMedida(String unidadMedida) {
        this.unidadMedida = unidadMedida;
    }

    public int getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(int stockMinimo) {
        this.stockMinimo = stockMinimo;
        this.estado = calcularEstado(this.cantidadDisponible, stockMinimo);
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
