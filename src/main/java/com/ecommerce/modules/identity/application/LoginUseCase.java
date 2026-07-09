package com.ecommerce.modules.identity.application;

import com.ecommerce.modules.identity.application.dto.*;
import com.ecommerce.modules.identity.domain.*;
import com.ecommerce.modules.shared.domain.UnauthorizedException;
import com.ecommerce.modules.shared.infrastructure.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoginUseCase {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public AuthResponse execute(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail().toLowerCase().trim(),
                            request.getPassword()
                    )
            );

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            if (!user.isEnabled()) {
                throw new UnauthorizedException("Account is disabled");
            }

            String accessToken = jwtTokenProvider.generateAccessToken(userDetails);
            String refreshTokenValue = jwtTokenProvider.generateRefreshToken();
            long accessExpiration = jwtTokenProvider.getAccessTokenExpiration();

            RefreshToken refreshToken = new RefreshToken(
                    refreshTokenValue,
                    user.getId(),
                    LocalDateTime.now().plusSeconds(jwtTokenProvider.getRefreshTokenExpiration() / 1000)
            );
            refreshTokenRepository.save(refreshToken);

            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshTokenValue)
                    .expiresIn(accessExpiration / 1000)
                    .tokenType("Bearer")
                    .build();
        } catch (BadCredentialsException e) {
            throw new UnauthorizedException("Invalid email or password");
        }
    }
}
