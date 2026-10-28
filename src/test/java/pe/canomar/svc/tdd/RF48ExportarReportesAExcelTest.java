package pe.canomar.svc.tdd;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pe.canomar.svc.comun.Calculos;
import pe.canomar.svc.comun.Calculos.LoteKg;
import pe.canomar.svc.comun.ConfirmacionException;
import pe.canomar.svc.comun.Fechas;
import pe.canomar.svc.comun.NegocioException;
import pe.canomar.svc.modelo.*;
import pe.canomar.svc.seguridad.UsuarioSesion;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * RF48 Exportar reportes a Excel — pruebas TDD de la clase ReporteServiceImpl.
 * Qué decide: cambia estado. Un método por caso de prueba (CP) de la ficha del Anexo B.
 */
@DisplayName("RF48 Exportar reportes a Excel")
class RF48ExportarReportesAExcelTest extends TddBase {

    /** RF48-CP01 (E1). Excel con hoja «Ventas», encabezado en la fila 3 y 2 filas de datos. */
    @Test
    @DisplayName("RF48-CP01 Exportar ventas del 27/10.")
    void rf48_cp01_excelVentas() throws Exception {
        // ARRANGE
        LocalDate dia = LocalDate.of(2026, 10, 27);
        when(ventaDao.listarEntre(dia, dia)).thenReturn(List.of(venta(10, null, "CONTADO", "59.20", "VIGENTE", null), venta(11, 1, "CREDITO", "320.00", "VIGENTE", null)));
        // ACT
        byte[] xlsx = reporteService().excelVentas(dia, dia);
        // ASSERT
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            Sheet sh = wb.getSheet("Ventas");
            assertNotNull(sh);
            assertEquals("Total (S/)", sh.getRow(2).getCell(8).getStringCellValue());
            assertEquals(320.00, sh.getRow(4).getCell(8).getNumericCellValue(), 0.001);
        }
    }

    /** RF48-CP02 (E2). Excel con 2 hojas: «Cierre 2026-09» y «Por producto». */
    @Test
    @DisplayName("RF48-CP02 Exportar el cierre mensual.")
    void rf48_cp02_excelCierreDosHojas() throws Exception {
        // ARRANGE
        YearMonth mes = YearMonth.of(2026, 9);
        // ACT
        byte[] xlsx = reporteService().excelCierreMensual(mes);
        // ASSERT
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            assertEquals(2, wb.getNumberOfSheets());
            assertEquals("Cierre 2026-09", wb.getSheetName(0));
            assertEquals("Por producto", wb.getSheetName(1));
        }
    }

    /** RF48-CP03 (E3). La fila de la corvina tiene margen 18.8. */
    @Test
    @DisplayName("RF48-CP03 Exportar ganancia por producto.")
    void rf48_cp03_excelGananciaProducto() throws Exception {
        // ARRANGE
        LocalDate desde = LocalDate.of(2026, 10, 1), hasta = LocalDate.of(2026, 10, 31);
        when(ventaDao.gananciaPorProducto(desde, hasta)).thenReturn(List.of(new GananciaProducto("Corvina", d("20.00"), d("640.00"), d("520.00"))));
        // ACT
        byte[] xlsx = reporteService().excelGananciaProducto(desde, hasta);
        // ASSERT
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            Row fila = wb.getSheetAt(0).getRow(3);
            assertEquals("Corvina", fila.getCell(0).getStringCellValue());
            assertEquals(18.8, fila.getCell(5).getNumericCellValue(), 0.001);
        }
    }

    /** RF48-CP04 (E4). Mensaje «Revise el rango de fechas». */
    @Test
    @DisplayName("RF48-CP04 Exportar ventas del 31/10 al 01/10.")
    void rf48_cp04_rangoInvertido() throws Exception {
        // ARRANGE
        LocalDate desde = LocalDate.of(2026, 10, 31), hasta = LocalDate.of(2026, 10, 1);
        // ACT
        NegocioException e = assertThrows(NegocioException.class, () -> reporteService().excelVentas(desde, hasta));
        // ASSERT
        assertEquals("Revise el rango de fechas", e.getMessage());
    }
}
