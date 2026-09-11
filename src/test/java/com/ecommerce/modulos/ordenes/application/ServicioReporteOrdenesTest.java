package com.ecommerce.modulos.ordenes.application;

import com.ecommerce.modulos.compartido.domain.Dinero;
import com.ecommerce.modulos.ordenes.domain.EstadoOrden;
import com.ecommerce.modulos.ordenes.domain.Orden;
import com.ecommerce.modulos.ordenes.domain.RepositorioOrden;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServicioReporteOrdenesTest {

    @Mock
    private RepositorioOrden repositorioOrden;

    private ServicioReporteOrdenes servicio;

    private UUID idTienda;

    private void setUp() {
        servicio = new ServicioReporteOrdenes(repositorioOrden);
        idTienda = UUID.randomUUID();
    }

    private static Orden orden(String numero, String cliente, String monto, EstadoOrden estado) {
        Orden orden = new Orden();
        orden.setNumeroOrden(numero);
        orden.setNombreCliente(cliente);
        orden.setTotal(Dinero.of(new BigDecimal(monto), "USD"));
        orden.setEstado(estado);
        return orden;
    }

    @Test
    void generaUnaFilaPorOrdenConEncabezado() throws Exception {
        setUp();
        List<Orden> ordenes = List.of(
                orden("A1", "Ana", "10.00", EstadoOrden.PAID),
                orden("A2", "Beto", "20.00", EstadoOrden.SHIPPED));
        when(repositorioOrden.findAllByIdTienda(eq(idTienda), any(Pageable.class)))
                .thenReturn(new PageImpl<>(ordenes, PageRequest.of(0, 500), ordenes.size()));

        byte[] reporte = servicio.generarReporteExcel(idTienda);

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(reporte))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals("ID Orden", sheet.getRow(0).getCell(0).getStringCellValue());

            Row fila1 = sheet.getRow(1);
            assertEquals("A1", fila1.getCell(0).getStringCellValue());
            assertEquals("Ana", fila1.getCell(1).getStringCellValue());
            assertEquals("PAID", fila1.getCell(3).getStringCellValue());

            Row fila2 = sheet.getRow(2);
            assertEquals("A2", fila2.getCell(0).getStringCellValue());
            assertNull(sheet.getRow(3));
        }
        verify(repositorioOrden, times(1)).findAllByIdTienda(eq(idTienda), any(Pageable.class));
    }

    @Test
    void recorreTodasLasPaginasSinCargarlasTodasJuntas() throws Exception {
        setUp();
        List<Orden> paginaUno = List.of(orden("P1", "Uno", "1.00", EstadoOrden.PENDING));
        List<Orden> paginaDos = List.of(orden("P2", "Dos", "2.00", EstadoOrden.PENDING));
        // total=501 con tamaño de página 500 -> 2 páginas, para forzar que el servicio pida
        // la segunda en vez de cortar tras la primera.
        org.springframework.data.domain.Sort ordenPorFecha = org.springframework.data.domain.Sort.by("creadoEn").descending();
        when(repositorioOrden.findAllByIdTienda(eq(idTienda), eq(PageRequest.of(0, 500, ordenPorFecha))))
                .thenReturn(new PageImpl<>(paginaUno, PageRequest.of(0, 500, ordenPorFecha), 501));
        when(repositorioOrden.findAllByIdTienda(eq(idTienda), eq(PageRequest.of(1, 500, ordenPorFecha))))
                .thenReturn(new PageImpl<>(paginaDos, PageRequest.of(1, 500, ordenPorFecha), 501));

        byte[] reporte = servicio.generarReporteExcel(idTienda);

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(reporte))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals("P1", sheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals("P2", sheet.getRow(2).getCell(0).getStringCellValue());
        }
        verify(repositorioOrden, times(2)).findAllByIdTienda(eq(idTienda), any(Pageable.class));
    }

    @Test
    void sinOrdenesGeneraSoloElEncabezado() throws Exception {
        setUp();
        when(repositorioOrden.findAllByIdTienda(eq(idTienda), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 500), 0));

        byte[] reporte = servicio.generarReporteExcel(idTienda);

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(reporte))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals("ID Orden", sheet.getRow(0).getCell(0).getStringCellValue());
            assertNull(sheet.getRow(1));
        }
    }
}
