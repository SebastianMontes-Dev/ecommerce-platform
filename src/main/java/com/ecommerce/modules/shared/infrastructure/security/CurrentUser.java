package com.ecommerce.modules.shared.infrastructure.security;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.*;

@Target({ElementType.PARAMETER, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal(expression = "@customUserDetailsService.loadUserByUsername(#this.username)")
public @interface CurrentUser {
}
