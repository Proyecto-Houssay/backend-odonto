package com.proyectohoussay.odonto.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Entity representing administrative and auxiliary staff members of the clinic.
 * Can be linked to a {@link Usuario} for system login capabilities.
 */
@Entity
@Table(name = "personal")
public class Personal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres.")
    @Column(nullable = false, length = 100)
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio.")
    @Size(max = 100, message = "El apellido no puede superar 100 caracteres.")
    @Column(nullable = false, length = 100)
    private String apellido;

    @NotBlank(message = "El DNI es obligatorio.")
    @Size(max = 20, message = "El DNI no puede superar 20 caracteres.")
    @Column(nullable = false, unique = true, length = 20)
    private String dni;

    @Size(max = 100, message = "El email no puede superar 100 caracteres.")
    @Column(length = 100)
    private String email;

    @Size(max = 20, message = "El teléfono no puede superar 20 caracteres.")
    @Column(length = 20)
    private String telefono;

    @NotNull(message = "El cargo es obligatorio.")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CargoPersonal cargo;

    @Column(name = "fecha_ingreso")
    private LocalDate fechaIngreso;

    @Size(max = 100, message = "El horario laboral no puede superar 100 caracteres.")
    @Column(name = "horario_laboral", length = 100)
    private String horarioLaboral;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "usuario_id")
    private Long usuarioId;

    public Personal() {
    }

    public Personal(
            String nombre,
            String apellido,
            String dni,
            String email,
            String telefono,
            CargoPersonal cargo,
            LocalDate fechaIngreso,
            String horarioLaboral,
            Long usuarioId
    ) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.dni = dni;
        this.email = email;
        this.telefono = telefono;
        this.cargo = cargo;
        this.fechaIngreso = fechaIngreso != null ? fechaIngreso : LocalDate.now();
        this.horarioLaboral = horarioLaboral;
        this.activo = true;
        this.usuarioId = usuarioId;
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

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
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

    public CargoPersonal getCargo() {
        return cargo;
    }

    public void setCargo(CargoPersonal cargo) {
        this.cargo = cargo;
    }

    public LocalDate getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(LocalDate fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }

    public String getHorarioLaboral() {
        return horarioLaboral;
    }

    public void setHorarioLaboral(String horarioLaboral) {
        this.horarioLaboral = horarioLaboral;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }
}
