package com.ecommerce.modulos.inquilino.application;

import com.ecommerce.modulos.inquilino.domain.Inquilino;
import com.ecommerce.modulos.inquilino.domain.RepositorioInquilino;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioResolutorInquilinoTest {

    @Mock
    private RepositorioInquilino repositorioInquilino;

    @InjectMocks
    private ServicioResolutorInquilino servicioResolutorInquilino;

    @Test
    void debeDevolverIdTiendaCuandoElUsuarioEsPropietario() {
        UUID idUsuario = UUID.randomUUID();
        Inquilino inquilino = new Inquilino("Mi Tienda", "mi-tienda", idUsuario);
        inquilino.setId(UUID.randomUUID()); // Set the ID explicitly for testing
        when(repositorioInquilino.findByIdPropietario(idUsuario)).thenReturn(Optional.of(inquilino));

        Optional<UUID> resultado = servicioResolutorInquilino.resolverTiendaPropia(idUsuario);

        assertTrue(resultado.isPresent());
        assertEquals(inquilino.getId(), resultado.get());
    }

    @Test
    void debeDevolverVacioCuandoElUsuarioNoTieneTiendaPropia() {
        UUID idUsuario = UUID.randomUUID();
        when(repositorioInquilino.findByIdPropietario(idUsuario)).thenReturn(Optional.empty());

        Optional<UUID> resultado = servicioResolutorInquilino.resolverTiendaPropia(idUsuario);

        assertTrue(resultado.isEmpty());
    }
}
