package com.ecommerce.modulos.analiticas.application;

import com.ecommerce.modulos.analiticas.application.dto.ProductoTop;
import com.ecommerce.modulos.analiticas.application.dto.ResumenDashboard;
import com.ecommerce.modulos.analiticas.application.dto.VentaDiaria;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CasoUsoAnaliticasTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private CasoUsoAnaliticas casoUsoAnaliticas;

    private static final String FRAGMENTO_SQL_VENTAS_DIARIAS = "TO_CHAR(creado_en";
    private static final String FRAGMENTO_SQL_PRODUCTOS_TOP = "order_items";

    private UUID idTienda;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        casoUsoAnaliticas = new CasoUsoAnaliticas(jdbcTemplate);
        idTienda = UUID.randomUUID();
    }

    @Test
    void debeRetornarResumenCompletoCuandoHayDatosEnLasTresConsultas() throws Exception {
        UUID idProducto1 = UUID.randomUUID();
        UUID idProducto2 = UUID.randomUUID();

        ResultSet rsTotales = mock(ResultSet.class);
        when(rsTotales.next()).thenReturn(true);
        when(rsTotales.getBigDecimal("ventas_totales")).thenReturn(new BigDecimal("1000.00"));
        when(rsTotales.getInt("ordenes_totales")).thenReturn(4);

        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq(idTienda)))
                .thenAnswer(invocacion -> {
                    ResultSetExtractor<ResumenDashboard.ResumenDashboardBuilder> extractor = invocacion.getArgument(1);
                    return extractor.extractData(rsTotales);
                });

        ResultSet rsVentas = mock(ResultSet.class);
        when(rsVentas.next()).thenReturn(true, true, false);
        when(rsVentas.getString("fecha")).thenReturn("2026-09-01", "2026-09-02");
        when(rsVentas.getBigDecimal("monto")).thenReturn(new BigDecimal("300.00"), new BigDecimal("700.00"));
        when(rsVentas.getInt("ordenes")).thenReturn(1, 3);

        when(jdbcTemplate.query(contains(FRAGMENTO_SQL_VENTAS_DIARIAS), any(RowMapper.class), eq(idTienda)))
                .thenAnswer(invocacion -> {
                    RowMapper<VentaDiaria> mapper = invocacion.getArgument(1);
                    List<VentaDiaria> resultado = new ArrayList<>();
                    int fila = 0;
                    while (rsVentas.next()) {
                        resultado.add(mapper.mapRow(rsVentas, fila++));
                    }
                    return resultado;
                });

        ResultSet rsProductos = mock(ResultSet.class);
        when(rsProductos.next()).thenReturn(true, true, false);
        when(rsProductos.getString("product_id")).thenReturn(idProducto1.toString(), idProducto2.toString());
        when(rsProductos.getString("product_name")).thenReturn("Zapatilla", "Remera");
        when(rsProductos.getInt("cantidad_vendida")).thenReturn(10, 5);

        when(jdbcTemplate.query(contains(FRAGMENTO_SQL_PRODUCTOS_TOP), any(RowMapper.class), eq(idTienda)))
                .thenAnswer(invocacion -> {
                    RowMapper<ProductoTop> mapper = invocacion.getArgument(1);
                    List<ProductoTop> resultado = new ArrayList<>();
                    int fila = 0;
                    while (rsProductos.next()) {
                        resultado.add(mapper.mapRow(rsProductos, fila++));
                    }
                    return resultado;
                });

        // Act
        ResumenDashboard resumen = casoUsoAnaliticas.obtenerResumen(idTienda);

        // Assert
        assertEquals(0, new BigDecimal("1000.00").compareTo(resumen.getVentasTotalesMes()));
        assertEquals(4, resumen.getOrdenesTotalesMes());
        assertEquals(0, new BigDecimal("250.00").compareTo(resumen.getTicketPromedio()));

        assertEquals(2, resumen.getIngresosUltimos7Dias().size());
        assertEquals("2026-09-01", resumen.getIngresosUltimos7Dias().get(0).getFecha());
        assertEquals(0, new BigDecimal("700.00").compareTo(resumen.getIngresosUltimos7Dias().get(1).getMonto()));
        assertEquals(3, resumen.getIngresosUltimos7Dias().get(1).getCantidadOrdenes());

        assertEquals(2, resumen.getProductosMasVendidos().size());
        assertEquals(idProducto1, resumen.getProductosMasVendidos().get(0).getIdProducto());
        assertEquals("Zapatilla", resumen.getProductosMasVendidos().get(0).getNombreProducto());
        assertEquals(10, resumen.getProductosMasVendidos().get(0).getCantidadVendida());
        assertEquals(idProducto2, resumen.getProductosMasVendidos().get(1).getIdProducto());

        verify(jdbcTemplate).query(anyString(), any(ResultSetExtractor.class), eq(idTienda));
        verify(jdbcTemplate).query(contains(FRAGMENTO_SQL_VENTAS_DIARIAS), any(RowMapper.class), eq(idTienda));
        verify(jdbcTemplate).query(contains(FRAGMENTO_SQL_PRODUCTOS_TOP), any(RowMapper.class), eq(idTienda));
    }

    @Test
    void debeArmarBuilderVacioCuandoNoHayOrdenesEnElMes() throws Exception {
        ArgumentCaptor<ResultSetExtractor> captorExtractor = ArgumentCaptor.forClass(ResultSetExtractor.class);

        when(jdbcTemplate.query(anyString(), captorExtractor.capture(), eq(idTienda)))
                .thenReturn(ResumenDashboard.builder());
        stubearListasVacias();

        casoUsoAnaliticas.obtenerResumen(idTienda);

        ResultSet rsSinFilas = mock(ResultSet.class);
        when(rsSinFilas.next()).thenReturn(false);

        Object builderCapturado = captorExtractor.getValue().extractData(rsSinFilas);
        ResumenDashboard resultado = ((ResumenDashboard.ResumenDashboardBuilder) builderCapturado).build();

        assertNull(resultado.getVentasTotalesMes());
        assertEquals(0, resultado.getOrdenesTotalesMes());
        assertNull(resultado.getTicketPromedio());
    }

    @Test
    void debeCalcularTicketPromedioComoCeroCuandoNoHayOrdenesEnElDivisor() throws Exception {
        ArgumentCaptor<ResultSetExtractor> captorExtractor = ArgumentCaptor.forClass(ResultSetExtractor.class);

        when(jdbcTemplate.query(anyString(), captorExtractor.capture(), eq(idTienda)))
                .thenReturn(ResumenDashboard.builder());
        stubearListasVacias();

        casoUsoAnaliticas.obtenerResumen(idTienda);

        ResultSet rs = mock(ResultSet.class);
        when(rs.next()).thenReturn(true);
        when(rs.getBigDecimal("ventas_totales")).thenReturn(new BigDecimal("500.00"));
        when(rs.getInt("ordenes_totales")).thenReturn(0);

        ResumenDashboard resultado = ((ResumenDashboard.ResumenDashboardBuilder) captorExtractor.getValue().extractData(rs)).build();

        assertEquals(0, BigDecimal.ZERO.compareTo(resultado.getTicketPromedio()));
    }

    @Test
    void debeRedondearTicketPromedioConHalfUpCuandoLaDivisionNoEsExacta() throws Exception {
        ArgumentCaptor<ResultSetExtractor> captorExtractor = ArgumentCaptor.forClass(ResultSetExtractor.class);

        when(jdbcTemplate.query(anyString(), captorExtractor.capture(), eq(idTienda)))
                .thenReturn(ResumenDashboard.builder());
        stubearListasVacias();

        casoUsoAnaliticas.obtenerResumen(idTienda);

        ResultSet rs = mock(ResultSet.class);
        when(rs.next()).thenReturn(true);
        when(rs.getBigDecimal("ventas_totales")).thenReturn(new BigDecimal("100.00"));
        when(rs.getInt("ordenes_totales")).thenReturn(3);

        ResumenDashboard resultado = ((ResumenDashboard.ResumenDashboardBuilder) captorExtractor.getValue().extractData(rs)).build();

        // 100 / 3 = 33.333... -> HALF_UP a 2 decimales = 33.33
        assertEquals(0, new BigDecimal("33.33").compareTo(resultado.getTicketPromedio()));
    }

    @Test
    void debeMapearVentaDiariaCorrectamenteDesdeElResultSet() throws Exception {
        ArgumentCaptor<RowMapper> captorMapper = ArgumentCaptor.forClass(RowMapper.class);

        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq(idTienda)))
                .thenReturn(ResumenDashboard.builder());
        when(jdbcTemplate.query(contains(FRAGMENTO_SQL_VENTAS_DIARIAS), captorMapper.capture(), eq(idTienda)))
                .thenReturn(List.of());
        when(jdbcTemplate.query(contains(FRAGMENTO_SQL_PRODUCTOS_TOP), any(RowMapper.class), eq(idTienda)))
                .thenReturn(List.of());

        casoUsoAnaliticas.obtenerResumen(idTienda);

        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("fecha")).thenReturn("2026-09-05");
        when(rs.getBigDecimal("monto")).thenReturn(new BigDecimal("150.50"));
        when(rs.getInt("ordenes")).thenReturn(2);

        VentaDiaria ventaDiaria = (VentaDiaria) captorMapper.getValue().mapRow(rs, 0);

        assertEquals("2026-09-05", ventaDiaria.getFecha());
        assertEquals(0, new BigDecimal("150.50").compareTo(ventaDiaria.getMonto()));
        assertEquals(2, ventaDiaria.getCantidadOrdenes());
    }

    @Test
    void debeRetornarListaVaciaDeVentasDiariasCuandoNoHayVentasEnLosUltimos7Dias() {
        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq(idTienda)))
                .thenReturn(ResumenDashboard.builder());
        when(jdbcTemplate.query(contains(FRAGMENTO_SQL_VENTAS_DIARIAS), any(RowMapper.class), eq(idTienda)))
                .thenReturn(List.of());
        when(jdbcTemplate.query(contains(FRAGMENTO_SQL_PRODUCTOS_TOP), any(RowMapper.class), eq(idTienda)))
                .thenReturn(List.of());

        ResumenDashboard resumen = casoUsoAnaliticas.obtenerResumen(idTienda);

        assertNotNull(resumen.getIngresosUltimos7Dias());
        assertTrue(resumen.getIngresosUltimos7Dias().isEmpty());
    }

    @Test
    void debeMapearProductoTopCorrectamenteConvirtiendoElIdAUUID() throws Exception {
        UUID idProducto = UUID.randomUUID();
        ArgumentCaptor<RowMapper> captorMapper = ArgumentCaptor.forClass(RowMapper.class);

        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq(idTienda)))
                .thenReturn(ResumenDashboard.builder());
        when(jdbcTemplate.query(contains(FRAGMENTO_SQL_VENTAS_DIARIAS), any(RowMapper.class), eq(idTienda)))
                .thenReturn(List.of());
        when(jdbcTemplate.query(contains(FRAGMENTO_SQL_PRODUCTOS_TOP), captorMapper.capture(), eq(idTienda)))
                .thenReturn(List.of());

        casoUsoAnaliticas.obtenerResumen(idTienda);

        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("product_id")).thenReturn(idProducto.toString());
        when(rs.getString("product_name")).thenReturn("Campera");
        when(rs.getInt("cantidad_vendida")).thenReturn(7);

        ProductoTop productoTop = (ProductoTop) captorMapper.getValue().mapRow(rs, 0);

        assertEquals(idProducto, productoTop.getIdProducto());
        assertEquals("Campera", productoTop.getNombreProducto());
        assertEquals(7, productoTop.getCantidadVendida());
    }

    @Test
    void debeRetornarListaVaciaDeProductosTopCuandoNoHayVentas() {
        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq(idTienda)))
                .thenReturn(ResumenDashboard.builder());
        when(jdbcTemplate.query(contains(FRAGMENTO_SQL_VENTAS_DIARIAS), any(RowMapper.class), eq(idTienda)))
                .thenReturn(List.of());
        when(jdbcTemplate.query(contains(FRAGMENTO_SQL_PRODUCTOS_TOP), any(RowMapper.class), eq(idTienda)))
                .thenReturn(List.of());

        ResumenDashboard resumen = casoUsoAnaliticas.obtenerResumen(idTienda);

        assertNotNull(resumen.getProductosMasVendidos());
        assertTrue(resumen.getProductosMasVendidos().isEmpty());
    }

    @Test
    void debeEnviarIdTiendaComoParametroALasTresConsultasParaAislamientoPorTenant() {
        stubearListasVacias();
        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq(idTienda)))
                .thenReturn(ResumenDashboard.builder());

        casoUsoAnaliticas.obtenerResumen(idTienda);

        verify(jdbcTemplate).query(anyString(), any(ResultSetExtractor.class), eq(idTienda));
        verify(jdbcTemplate).query(contains(FRAGMENTO_SQL_VENTAS_DIARIAS), any(RowMapper.class), eq(idTienda));
        verify(jdbcTemplate).query(contains(FRAGMENTO_SQL_PRODUCTOS_TOP), any(RowMapper.class), eq(idTienda));
    }

    private void stubearListasVacias() {
        when(jdbcTemplate.query(contains(FRAGMENTO_SQL_VENTAS_DIARIAS), any(RowMapper.class), eq(idTienda)))
                .thenReturn(List.of());
        when(jdbcTemplate.query(contains(FRAGMENTO_SQL_PRODUCTOS_TOP), any(RowMapper.class), eq(idTienda)))
                .thenReturn(List.of());
    }
}
