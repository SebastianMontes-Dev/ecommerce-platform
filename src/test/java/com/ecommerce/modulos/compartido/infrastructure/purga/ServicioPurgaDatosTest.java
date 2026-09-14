package com.ecommerce.modulos.compartido.infrastructure.purga;

import com.ecommerce.modulos.compartido.infrastructure.outbox.RepositorioEventoOutbox;
import com.ecommerce.modulos.identidad.domain.RepositorioTokenActualizacion;
import com.ecommerce.modulos.pagos.domain.RepositorioEventoProcesado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioPurgaDatosTest {

    private static final int DIAS_RETENCION_OUTBOX = 30;
    private static final int DIAS_RETENCION_REFRESH_TOKENS = 7;
    private static final int DIAS_RETENCION_EVENTOS_PROCESADOS = 90;
    private static final Duration MARGEN_TOLERADO = Duration.ofSeconds(5);

    @Mock private RepositorioEventoOutbox repositorioEventoOutbox;
    @Mock private RepositorioTokenActualizacion repositorioTokenActualizacion;
    @Mock private RepositorioEventoProcesado repositorioEventoProcesado;

    @InjectMocks
    private ServicioPurgaDatos servicioPurgaDatos;

    @BeforeEach
    void configurarRetenciones() {
        ReflectionTestUtils.setField(servicioPurgaDatos, "diasRetencionOutbox", DIAS_RETENCION_OUTBOX);
        ReflectionTestUtils.setField(servicioPurgaDatos, "diasRetencionRefreshTokens", DIAS_RETENCION_REFRESH_TOKENS);
        ReflectionTestUtils.setField(servicioPurgaDatos, "diasRetencionEventosProcesados", DIAS_RETENCION_EVENTOS_PROCESADOS);
    }

    @Test
    void purgarDebeLlamarALosTresRepositoriosConElCorteDeFechaSegunSuRetencionConfigurada() {
        when(repositorioEventoOutbox.eliminarProcesadosAntesDe(any())).thenReturn(3);
        when(repositorioTokenActualizacion.eliminarRevocadosOExpiradosAntesDe(any())).thenReturn(5);
        when(repositorioEventoProcesado.eliminarAntesDe(any())).thenReturn(2);

        servicioPurgaDatos.purgar();

        ArgumentCaptor<LocalDateTime> corteOutbox = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> corteRefreshTokens = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> corteEventosProcesados = ArgumentCaptor.forClass(LocalDateTime.class);

        verify(repositorioEventoOutbox).eliminarProcesadosAntesDe(corteOutbox.capture());
        verify(repositorioTokenActualizacion).eliminarRevocadosOExpiradosAntesDe(corteRefreshTokens.capture());
        verify(repositorioEventoProcesado).eliminarAntesDe(corteEventosProcesados.capture());

        assertCorteDentroDelMargen(corteOutbox.getValue(), DIAS_RETENCION_OUTBOX);
        assertCorteDentroDelMargen(corteRefreshTokens.getValue(), DIAS_RETENCION_REFRESH_TOKENS);
        assertCorteDentroDelMargen(corteEventosProcesados.getValue(), DIAS_RETENCION_EVENTOS_PROCESADOS);
    }

    private void assertCorteDentroDelMargen(LocalDateTime corteCapturado, int diasRetencionEsperados) {
        LocalDateTime corteEsperado = LocalDateTime.now().minusDays(diasRetencionEsperados);
        assertThat(Duration.between(corteEsperado, corteCapturado).abs()).isLessThan(MARGEN_TOLERADO);
    }
}
