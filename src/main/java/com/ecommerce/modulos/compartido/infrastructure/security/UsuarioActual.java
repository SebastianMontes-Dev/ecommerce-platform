package com.ecommerce.modulos.compartido.infrastructure.security;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.*;

@Target({ElementType.PARAMETER, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal(expression = "@servicioDetallesUsuarioPersonalizado.loadUserByUsername(#this.username)")
public @interface UsuarioActual {
}
