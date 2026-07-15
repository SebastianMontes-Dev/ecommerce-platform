package com.ecommerce.modulos.identidad.domain;

import com.ecommerce.modulos.compartido.infrastructure.RepositorioJpaBase;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepositorioUsuario extends RepositorioJpaBase<Usuario> {

    Optional<Usuario> findByCorreo(String correo);

    boolean existsByCorreo(String correo);
}
