package com.ecommerce.modules.identity.application;

import com.ecommerce.modules.identity.domain.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class CustomUserDetails implements UserDetails {

    private final UUID userId;
    private final String correo;
    private final String contrasena;
    private final boolean enabled;
    private final Set<GrantedAuthority> authorities;

    public CustomUserDetails(User user) {
        this.userId = user.getId();
        this.correo = user.getCorreo();
        this.contrasena = user.getHashContrasena();
        this.enabled = user.isEnabled();
        this.authorities = user.getRoles().stream()
                .map(rol -> new SimpleGrantedAuthority("ROLE_" + rol.name()))
                .collect(Collectors.toSet());
    }

    public UUID getUserId() {
        return userId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return contrasena;
    }

    @Override
    public String getUsername() {
        return correo;
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
        return enabled;
    }
}
