package com.ecommerce.modules.identity.application;

import com.ecommerce.modules.identity.application.dto.*;
import com.ecommerce.modules.identity.domain.*;
import com.ecommerce.modules.shared.domain.BusinessRuleViolationException;
import com.ecommerce.modules.shared.domain.DuplicateResourceException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RegisterUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse execute(RegisterRequest request) {
        List<String> violations = new java.util.ArrayList<>();

        if (!request.getContrasena().equals(request.getConfirmPassword())) {
            violations.add("Las contraseñas no coinciden");
        }

        if (userRepository.existsByCorreo(request.getCorreo())) {
            violations.add("El correo ya está registrado");
        }

        if (!violations.isEmpty()) {
            throw new BusinessRuleViolationException(violations);
        }

        User user = new User(
                request.getCorreo().toLowerCase().trim(),
                passwordEncoder.encode(request.getContrasena()),
                request.getNombre(),
                request.getApellido()
        );
        user.addRole(UserRole.CUSTOMER);

        user = userRepository.save(user);

        return mapToResponse(user);
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .correo(user.getCorreo())
                .nombre(user.getNombre())
                .apellido(user.getApellido())
                .fullName(user.getFullName())
                .emailVerified(user.isEmailVerified())
                .enabled(user.isEnabled())
                .roles(user.getRoles().stream().map(Enum::name).collect(Collectors.toSet()))
                .creadoEn(user.getCreadoEn())
                .build();
    }
}
