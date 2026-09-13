package com.ecommerce.modulos.compartido.infrastructure.purga;

import com.ecommerce.modulos.compartido.infrastructure.outbox.RepositorioEventoOutbox;
import com.ecommerce.modulos.identidad.domain.RepositorioTokenActualizacion;
import com.ecommerce.modulos.pagos.domain.RepositorioEventoProcesado;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioPurgaDatosTest {

    @Mock private RepositorioEventoOutbox repositorioEventoOutbox;
    @Mock private RepositorioTokenActualizacion repositorioTokenActualizacion;
    @Mock private RepositorioEventoProcesado repositorioEventoProcesado;

    @InjectMocks
    private ServicioPurgaDatos servicioPurgaDatos;

    @Test
    void purgarDebeLlamarALosTresRepositoriosConUnCorteDeFechaEnElPasado() {
        when(repositorioEventoOutbox.eliminarProcesadosAntesDe(any())).thenReturn(3);
        when(repositorioTokenActualizacion.eliminarRevocadosOExpiradosAntesDe(any())).thenReturn(5);
        when(repositorioEventoProcesado.eliminarAntesDe(any())).thenReturn(2);

        servicioPurgaDatos.purgar();

        verify(repositorioEventoOutbox).eliminarProcesadosAntesDe(argThat(fecha -> fecha.isBefore(LocalDateTime.now())));
        verify(repositorioTokenActualizacion).eliminarRevocadosOExpiradosAntesDe(argThat(fecha -> fecha.isBefore(LocalDateTime.now())));
        verify(repositorioEventoProcesado).eliminarAntesDe(argThat(fecha -> fecha.isBefore(LocalDateTime.now())));
    }
}
