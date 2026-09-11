package com.ecommerce.modulos.ordenes.application;

import com.ecommerce.modulos.ordenes.domain.Orden;
import com.ecommerce.modulos.ordenes.domain.RepositorioOrden;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServicioReporteOrdenes {

    // Filas que SXSSFWorkbook mantiene en memoria antes de volcarlas a un archivo temporal.
    private static final int VENTANA_EN_MEMORIA = 100;
    // Órdenes por página leída de la DB: evita traer la tienda entera de una sola consulta.
    private static final int TAMANO_PAGINA = 500;

    private final RepositorioOrden repositorioOrden;

    /**
     * Antes: {@code findAllByIdTienda(idTienda)} sin paginar (todas las órdenes de la tienda,
     * como entidades JPA completas) + {@code XSSFWorkbook} (el libro entero como DOM en
     * memoria). Con una tienda de varios miles de pedidos, esto es candidato directo a
     * {@code OutOfMemoryError}. Ahora se pagina la lectura y se escribe con
     * {@link SXSSFWorkbook}, que solo retiene {@link #VENTANA_EN_MEMORIA} filas a la vez.
     */
    @Transactional(readOnly = true)
    public byte[] generarReporteExcel(UUID idTienda) {
        SXSSFWorkbook workbook = new SXSSFWorkbook(VENTANA_EN_MEMORIA);
        try {
            Sheet sheet = workbook.createSheet("Reporte Ordenes");
            escribirEncabezado(sheet);
            escribirFilas(sheet, idTienda);

            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                workbook.write(out);
                return out.toByteArray();
            }
        } catch (IOException e) {
            throw new RuntimeException("Error al generar el reporte de Excel", e);
        } finally {
            // dispose() borra los archivos temporales de las filas que salieron de la
            // ventana; close() por sí solo NO los limpia (gotcha conocido de SXSSFWorkbook).
            workbook.dispose();
            try {
                workbook.close();
            } catch (IOException ignored) {
                // ya escribimos el resultado; un fallo cerrando el workbook no lo invalida.
            }
        }
    }

    private void escribirEncabezado(Sheet sheet) {
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("ID Orden");
        headerRow.createCell(1).setCellValue("Cliente");
        headerRow.createCell(2).setCellValue("Monto");
        headerRow.createCell(3).setCellValue("Estado");
    }

    private void escribirFilas(Sheet sheet, UUID idTienda) {
        int filaActual = 1;
        Pageable pageable = PageRequest.of(0, TAMANO_PAGINA, Sort.by("creadoEn").descending());
        Page<Orden> pagina;
        do {
            pagina = repositorioOrden.findAllByIdTienda(idTienda, pageable);
            for (Orden orden : pagina.getContent()) {
                escribirFila(sheet, filaActual++, orden);
            }
            pageable = pageable.next();
        } while (pagina.hasNext());
    }

    private void escribirFila(Sheet sheet, int filaIdx, Orden orden) {
        Row row = sheet.createRow(filaIdx);
        row.createCell(0).setCellValue(orden.getNumeroOrden() != null ? orden.getNumeroOrden() : "");
        row.createCell(1).setCellValue(orden.getNombreCliente() != null ? orden.getNombreCliente() : "");

        String monto = "";
        if (orden.getTotal() != null && orden.getTotal().getMonto() != null) {
            monto = orden.getTotal().getMonto().toString();
        }
        row.createCell(2).setCellValue(monto);
        row.createCell(3).setCellValue(orden.getEstado() != null ? orden.getEstado().name() : "");
    }
}
