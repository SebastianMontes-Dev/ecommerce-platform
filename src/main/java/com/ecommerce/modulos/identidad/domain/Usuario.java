package com.ecommerce.modulos.identidad.domain;

import com.ecommerce.modulos.compartido.domain.EntidadAuditableBase;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
public class Usuario extends EntidadAuditableBase {

    @Column(name = "correo", unique = true, nullable = false)
    private String correo;

    @Column(name = "hash_contrasena", nullable = false)
    private String hashContrasena;

    @Column(name = "first_name")
    private String nombre;

    @Column(name = "last_name")
    private String apellido;

    @Column(name = "enabled")
    private boolean enabled = true;

    @Column(name = "email_verified")
    private boolean emailVerified = false;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "rol")
    private Set<RolUsuario> roles = new HashSet<>();

    public Usuario(String correo, String hashContrasena, String nombre, String apellido) {
        this.correo = correo;
        this.hashContrasena = hashContrasena;
        this.nombre = nombre;
        this.apellido = apellido;
        this.enabled = true;
        this.emailVerified = false;
        this.roles = new HashSet<>();
    }

    public void verifyEmail() {
        this.emailVerified = true;
    }

    public void disable() {
        this.enabled = false;
    }

    public void enable() {
        this.enabled = true;
    }

    public void changePassword(String newPasswordHash) {
        this.hashContrasena = newPasswordHash;
    }

    public void addRole(RolUsuario rol) {
        this.roles.add(rol);
    }

    public void removeRole(RolUsuario rol) {
        this.roles.remove(rol);
    }

    public boolean hasRole(RolUsuario rol) {
        return this.roles.contains(rol);
    }

    public String getNombreCompleto() {
        if (nombre != null && apellido != null) {
            return nombre + " " + apellido;
        }
        if (nombre != null) return nombre;
        if (apellido != null) return apellido;
        return correo;
    }
}
