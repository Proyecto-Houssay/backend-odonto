package com.proyectohoussay.odonto.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "odontologos")
public class Odontologo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres.")
    @Column(nullable = false, length = 100)
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio.")
    @Size(max = 100, message = "El apellido no puede superar los 100 caracteres.")
    @Column(nullable = false, length = 100)
    private String apellido;

    @NotBlank(message = "La matrícula profesional es obligatoria.")
    @Size(max = 50, message = "La matrícula no puede superar los 50 caracteres.")
    @Column(nullable = false, unique = true, length = 50)
    private String matricula;

    @Size(max = 100, message = "El email no puede superar los 100 caracteres.")
    @Column(length = 100)
    private String email;

    @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres.")
    @Column(length = 20)
    private String telefono;

    private boolean activo = true;

    @NotNull(message = "La especialidad es obligatoria.")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "especialidad_id", nullable = false)
    private Especialidad especialidad;

    public Odontologo() {
    }

    public Odontologo(String nombre, String apellido, String matricula, String email, String telefono, Especialidad especialidad) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.matricula = matricula;
        this.email = email;
        this.telefono = telefono;
        this.especialidad = especialidad;
        this.activo = true;
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

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getMatricula() {
        return matricula;
    }

    public void setMatricula(String matricula) {
        this.matricula = matricula;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public Especialidad getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(Especialidad especialidad) {
        this.especialidad = especialidad;
    }
}
