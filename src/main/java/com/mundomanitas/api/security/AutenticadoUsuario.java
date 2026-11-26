package com.mundomanitas.api.security;

import com.mundomanitas.api.model.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Adapta la entidad {@enlace Usuario} al modelo de Spring Security.
 */
public class AutenticadoUsuario implements UserDetails {

    private final UUID id;
    private final String email;
    private final String contrasenaHash;
    private final String nombreCompleto;
    private final String rol;
    private final boolean activo;

    public AutenticadoUsuario(Usuario usuario) {
        this.id = usuario.getId();
        this.email = usuario.getEmail();
        this.contrasenaHash = usuario.getContrasenaHash();
        this.nombreCompleto = usuario.getNombreCompleto();
        this.rol = usuario.getRol();
        this.activo = usuario.isActivo();
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    @Override
    public String getPassword() {
        return contrasenaHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return activo;
    }
}
