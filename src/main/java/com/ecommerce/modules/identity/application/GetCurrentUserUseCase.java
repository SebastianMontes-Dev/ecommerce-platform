package com.ecommerce.modules.identity.application;

import com.ecommerce.modules.identity.application.dto.UserResponse;
import com.ecommerce.modules.identity.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetCurrentUserUseCase {

    private final UserRepository userRepository;

    public UserResponse execute() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails userDetails)) {
            return null;
        }

        return userRepository.findById(userDetails.getUserId())
                .map(this::mapToResponse)
                .orElse(null);
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .correo(user.getCorreo())
                .nombre(user.getNombre())
                .apellido(user.getApellido())
                .nombreCompleto(user.getNombreCompleto())
                .emailVerified(user.isEmailVerified())
                .enabled(user.isEnabled())
                .roles(user.getRoles().stream().map(Enum::name).collect(Collectors.toSet()))
                .creadoEn(user.getCreadoEn())
                .build();
    }
}
