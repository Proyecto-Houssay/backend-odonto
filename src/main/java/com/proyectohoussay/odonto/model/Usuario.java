package com.proyectohoussay.odonto.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.regex.Pattern;

@Entity
@Table(name = "usuarios")
public class Usuario {

    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();
    private static final Pattern BCRYPT_HASH = Pattern.compile("^\\$2[aby]\\$\\d{2}\\$.{53}$");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre de usuario es obligatorio.")
    @Size(max = 50, message = "El nombre de usuario no puede superar 50 caracteres.")
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 100)
    @Column(nullable = false)
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio.")
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String apellido;

    @NotBlank(message = "El email es obligatorio.")
    @Email(message = "El email no tiene un formato válido.")
    @Size(max = 254)
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank(message = "La contraseña es obligatoria.")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false, length = 60)
    private String password;

    @NotBlank(message = "El rol es obligatorio.")
    @Size(max = 30)
    @Column(nullable = false, length = 30)
    private String rol;
    private boolean activo = true;
    private String telefono;

    public Usuario() {
    }

    public Usuario(String nombre, String apellido, String email, String rol, boolean activo) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.rol = rol;
        this.activo = activo;
        this.username = email;
    }

    public Usuario(String username, String email, String password, String rol) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.rol = rol;
        this.nombre = username;
        this.apellido = "";
    }

    public Usuario(String username, String nombre, String apellido, String email, String password, String rol, boolean activo) {
        this.username = username;
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.password = password;
        this.rol = rol;
        this.activo = activo;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public void setPassword(String password) {
        this.password = password;
    }

    @PrePersist
    @PreUpdate
    private void hashPasswordBeforeSaving() {
        if (password != null && !BCRYPT_HASH.matcher(password).matches()) {
            password = PASSWORD_ENCODER.encode(password);
        }
    }
}
