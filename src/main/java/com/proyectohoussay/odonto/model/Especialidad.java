package com.proyectohoussay.odonto.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "especialidades")
public class Especialidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre de la especialidad es obligatorio.")
    @Size(max = 100, message = "El nombre de la especialidad no puede superar los 100 caracteres.")
    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Size(max = 255, message = "La descripción no puede superar los 255 caracteres.")
    @Column(length = 255)
    private String descripcion;

    @JsonIgnore
    @OneToMany(mappedBy = "especialidad", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private List<Odontologo> odontologos = new ArrayList<>();

    public Especialidad() {
    }

    public Especialidad(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
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

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public List<Odontologo> getOdontologos() {
        return Collections.unmodifiableList(odontologos);
    }

    public void addOdontologo(Odontologo odontologo) {
        if (odontologo != null) {
            this.odontologos.add(odontologo);
            odontologo.setEspecialidad(this);
        }
    }

    public void removeOdontologo(Odontologo odontologo) {
        if (odontologo != null) {
            this.odontologos.remove(odontologo);
            if (odontologo.getEspecialidad() == this) {
                odontologo.setEspecialidad(null);
            }
        }
    }
}
