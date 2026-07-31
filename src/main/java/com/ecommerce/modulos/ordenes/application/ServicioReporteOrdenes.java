package com.ecommerce.modulos.ordenes.application;

import com.ecommerce.modulos.ordenes.domain.Orden;
import com.ecommerce.modulos.ordenes.domain.RepositorioOrden;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServicioReporteOrdenes {

    private final RepositorioOrden repositorioOrden;

    @Transactional(readOnly = true)
    public byte[] generarReporteExcel(UUID idTienda) {
        List<Orden> ordenes = repositorioOrden.findAllByIdTienda(idTienda);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Reporte Ordenes");

            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("ID Orden");
            headerRow.createCell(1).setCellValue("Cliente");
            headerRow.createCell(2).setCellValue("Monto");
            headerRow.createCell(3).setCellValue("Estado");

            int rowIdx = 1;
            for (Orden orden : ordenes) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(orden.getNumeroOrden() != null ? orden.getNumeroOrden() : "");
                row.createCell(1).setCellValue(orden.getNombreCliente() != null ? orden.getNombreCliente() : "");
                
                String monto = "";
                if (orden.getTotal() != null && orden.getTotal().getMonto() != null) {
                    monto = orden.getTotal().getMonto().toString();
                }
                row.createCell(2).setCellValue(monto);
                row.createCell(3).setCellValue(orden.getEstado() != null ? orden.getEstado().name() : "");
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error al generar el reporte de Excel", e);
        }
    }
}
