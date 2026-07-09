package com.ecommerce.modules.identity.application;

import com.ecommerce.modules.identity.application.dto.AuthResponse;
import com.ecommerce.modules.identity.application.dto.RefreshTokenRequest;
import com.ecommerce.modules.identity.domain.*;
import com.ecommerce.modules.shared.domain.UnauthorizedException;
import com.ecommerce.modules.shared.infrastructure.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RefreshTokenUseCase {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Transactional
    public AuthResponse execute(RefreshTokenRequest request) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (!storedToken.isValid()) {
            throw UnauthorizedException.tokenExpired();
        }

        storedToken.revoke();
        refreshTokenRepository.save(storedToken);

        User user = userRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        CustomUserDetails userDetails = new CustomUserDetails(user);

        String newAccessToken = jwtTokenProvider.generateAccessToken(userDetails);
        String newRefreshTokenValue = jwtTokenProvider.generateRefreshToken();
        long accessExpiration = jwtTokenProvider.getAccessTokenExpiration();

        RefreshToken newRefreshToken = new RefreshToken(
                newRefreshTokenValue,
                user.getId(),
                LocalDateTime.now().plusSeconds(jwtTokenProvider.getRefreshTokenExpiration() / 1000)
        );
        refreshTokenRepository.save(newRefreshToken);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshTokenValue)
                .expiresIn(accessExpiration / 1000)
                .tokenType("Bearer")
                .build();
    }
}
