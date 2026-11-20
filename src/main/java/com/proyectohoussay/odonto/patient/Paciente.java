package com.proyectohoussay.odonto.patient;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "paciente")
public class Paciente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    private String apellido;

    @NotBlank(message = "El DNI es obligatorio")
    @Column(nullable = false, unique = true)
    private String dni;

    private String telefono;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El correo electrónico no tiene un formato válido")
    private String email;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @PastOrPresent(message = "La fecha de nacimiento no puede ser futura")
    private LocalDate fechaNacimiento;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "obra_social_id")
    private ObraSocial obraSocial;

    @Column(name = "numero_afiliado", length = 50)
    private String numeroAfiliado;

    @Column(name = "plan_cobertura", length = 100)
    private String planCobertura;

    @Column(nullable = true)
    private Boolean particular = true;

    @Embedded
    private ContactoEmergencia contactoEmergencia;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "paciente_alertas_medicas", joinColumns = @JoinColumn(name = "paciente_id"))
    @OrderColumn(name = "orden")
    @Column(name = "alerta", nullable = false, length = 200)
    private List<String> alertasMedicas = new ArrayList<>();

    @Column(name = "creado_en", nullable = true, updatable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = true)
    private LocalDateTime actualizadoEn;

    public Paciente() {
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

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public ObraSocial getObraSocial() { return obraSocial; }
    public void setObraSocial(ObraSocial obraSocial) { this.obraSocial = obraSocial; }
    public String getNumeroAfiliado() { return numeroAfiliado; }
    public void setNumeroAfiliado(String numeroAfiliado) { this.numeroAfiliado = numeroAfiliado; }
    public String getPlanCobertura() { return planCobertura; }
    public void setPlanCobertura(String planCobertura) { this.planCobertura = planCobertura; }
    public boolean isParticular() { return particular == null || particular; }
    public void setParticular(boolean particular) { this.particular = particular; }
    public ContactoEmergencia getContactoEmergencia() { return contactoEmergencia; }
    public void setContactoEmergencia(ContactoEmergencia contactoEmergencia) { this.contactoEmergencia = contactoEmergencia; }
    public List<String> getAlertasMedicas() { return alertasMedicas; }
    public void setAlertasMedicas(List<String> alertasMedicas) {
        this.alertasMedicas = alertasMedicas == null ? new ArrayList<>() : new ArrayList<>(alertasMedicas);
    }
    public LocalDateTime getCreadoEn() { return creadoEn; }
    public LocalDateTime getActualizadoEn() { return actualizadoEn; }

    @PrePersist
    void alCrear() {
        LocalDateTime ahora = LocalDateTime.now();
        creadoEn = ahora;
        actualizadoEn = ahora;
        if (particular == null) {
            particular = true;
        }
    }

    @PreUpdate
    void alActualizar() {
        actualizadoEn = LocalDateTime.now();
        if (particular == null) {
            particular = true;
        }
    }
}
