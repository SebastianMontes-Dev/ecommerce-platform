package com.ecommerce.modulos.analiticas.application;

import com.ecommerce.modulos.analiticas.application.dto.ProductoTop;
import com.ecommerce.modulos.analiticas.application.dto.ResumenDashboard;
import com.ecommerce.modulos.analiticas.application.dto.VentaDiaria;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CasoUsoAnaliticas {

    private final JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public ResumenDashboard obtenerResumen(UUID idTienda) {

        String sqlTotales = """
            SELECT 
                COALESCE(SUM(monto_total), 0) as ventas_totales, 
                COUNT(id) as ordenes_totales
            FROM ordenes 
            WHERE tenant_id = ? 
            AND estado IN ('PAID', 'PROCESSING', 'SHIPPED', 'DELIVERED')
            AND date_trunc('month', creado_en) = date_trunc('month', CURRENT_DATE)
        """;

        String sqlVentasDiarias = """
            SELECT 
                TO_CHAR(creado_en, 'YYYY-MM-DD') as fecha,
                COALESCE(SUM(monto_total), 0) as monto,
                COUNT(id) as ordenes
            FROM ordenes
            WHERE tenant_id = ? 
            AND estado IN ('PAID', 'PROCESSING', 'SHIPPED', 'DELIVERED')
            AND creado_en >= CURRENT_DATE - INTERVAL '6 days'
            GROUP BY TO_CHAR(creado_en, 'YYYY-MM-DD')
            ORDER BY fecha ASC
        """;

        String sqlProductosTop = """
            SELECT 
                product_id, 
                product_name, 
                SUM(cantidad) as cantidad_vendida
            FROM order_items oi
            JOIN ordenes o ON oi.order_id = o.id
            WHERE o.tenant_id = ? 
            AND o.estado IN ('PAID', 'PROCESSING', 'SHIPPED', 'DELIVERED')
            GROUP BY product_id, product_name
            ORDER BY cantidad_vendida DESC
            LIMIT 5
        """;

        // 1. Obtener totales
        ResumenDashboard.ResumenDashboardBuilder builder = jdbcTemplate.query(sqlTotales, rs -> {
            if (rs.next()) {
                BigDecimal ventas = rs.getBigDecimal("ventas_totales");
                int ordenes = rs.getInt("ordenes_totales");
                BigDecimal ticket = ordenes > 0 ? ventas.divide(new BigDecimal(ordenes), 2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;
                return ResumenDashboard.builder()
                        .ventasTotalesMes(ventas)
                        .ordenesTotalesMes(ordenes)
                        .ticketPromedio(ticket);
            }
            return ResumenDashboard.builder();
        }, idTienda);

        // 2. Obtener ventas diarias
        List<VentaDiaria> ventasDiarias = jdbcTemplate.query(sqlVentasDiarias, this::mapVentaDiaria, idTienda);
        builder.ingresosUltimos7Dias(ventasDiarias);

        // 3. Obtener top productos
        List<ProductoTop> productosTop = jdbcTemplate.query(sqlProductosTop, this::mapProductoTop, idTienda);
        builder.productosMasVendidos(productosTop);

        return builder.build();
    }

    private VentaDiaria mapVentaDiaria(ResultSet rs, int rowNum) throws SQLException {
        return new VentaDiaria(
                rs.getString("fecha"),
                rs.getBigDecimal("monto"),
                rs.getInt("ordenes")
        );
    }

    private ProductoTop mapProductoTop(ResultSet rs, int rowNum) throws SQLException {
        return new ProductoTop(
                UUID.fromString(rs.getString("product_id")),
                rs.getString("product_name"),
                rs.getInt("cantidad_vendida")
        );
    }
}
