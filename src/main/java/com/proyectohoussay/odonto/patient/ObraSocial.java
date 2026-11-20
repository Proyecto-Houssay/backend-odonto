package com.proyectohoussay.odonto.patient;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "obras_sociales")
public class ObraSocial {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank @Size(max = 120)
    @Column(nullable = false, unique = true, length = 120)
    private String nombre;
    @Size(max = 20) @Column(length = 20)
    private String sigla;
    @Size(max = 40) @Column(length = 40)
    private String codigo;
    @Column(nullable = false)
    private boolean activo = true;

    public ObraSocial() {}
    public ObraSocial(String nombre, String sigla, String codigo, boolean activo) {
        this.nombre = nombre; this.sigla = sigla; this.codigo = codigo; this.activo = activo;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getSigla() { return sigla; }
    public void setSigla(String sigla) { this.sigla = sigla; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
