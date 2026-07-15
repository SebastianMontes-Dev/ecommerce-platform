package com.ecommerce.modulos.identidad.domain;

import com.ecommerce.modulos.compartido.domain.EntidadBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
public class TokenActualizacion extends EntidadBase {

    @Column(name = "token", unique = true, nullable = false, length = 512)
    private String token;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "expira_en", nullable = false)
    private LocalDateTime expiraEn;

    @Column(name = "revoked")
    private boolean revoked = false;

    public TokenActualizacion(String token, UUID userId, LocalDateTime expiraEn) {
        this.token = token;
        this.userId = userId;
        this.expiraEn = expiraEn;
        this.revoked = false;
    }

    public boolean isValid() {
        return !revoked && expiraEn.isAfter(LocalDateTime.now());
    }

    public void revoke() {
        this.revoked = true;
    }
}
