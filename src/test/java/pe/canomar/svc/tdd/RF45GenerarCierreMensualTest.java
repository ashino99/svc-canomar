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
 * RF45 Generar cierre mensual — pruebas TDD de la clase ReporteServiceImpl.
 * Qué decide: calcula. Un método por caso de prueba (CP) de la ficha del Anexo B.
 */
@DisplayName("RF45 Generar cierre mensual")
class RF45GenerarCierreMensualTest extends TddBase {

    /** RF45-CP01 (E1). Ganancia S/ 5,800.00 y ventas sin factura S/ 16,000.00. */
    @Test
    @DisplayName("RF45-CP01 Generar el cierre de septiembre.")
    void rf45_cp01_resumenDelMes() throws Exception {
        // ARRANGE
        YearMonth mes = YearMonth.of(2026, 9);
        LocalDate desde = mes.atDay(1), hasta = mes.atEndOfMonth();
        when(ventaDao.totalVentas(desde, hasta)).thenReturn(d("25000.00"));
        when(ventaDao.costoVendido(desde, hasta)).thenReturn(d("18000.00"));
        when(loteDao.perdidas(desde, hasta)).thenReturn(d("300.00"));
        when(gastoDao.total(desde, hasta)).thenReturn(d("900.00"));
        when(facturaDao.totalEmitido(desde, hasta)).thenReturn(d("9000.00"));
        // ACT
        CierreMensual c = reporteService().cierreMensual(mes);
        // ASSERT
        assertEquals(d("5800.00"), c.ganancia());
        assertEquals(d("16000.00"), c.noFacturado());
    }

    /** RF45-CP02 (E2). Días con ventas sin cuadre = 1. */
    @Test
    @DisplayName("RF45-CP02 Generar el cierre.")
    void rf45_cp02_diasSinCuadre() throws Exception {
        // ARRANGE
        YearMonth mes = YearMonth.of(2026, 9);
        LocalDate dia = LocalDate.of(2026, 9, 15);
        when(ventaDao.cantidadVentas(dia, dia)).thenReturn(12);
        // ACT
        CierreMensual c = reporteService().cierreMensual(mes);
        // ASSERT
        assertEquals(1, c.diasSinCuadre());
    }

    /** RF45-CP03 (E3). Cierre marcado como parcial. */
    @Test
    @DisplayName("RF45-CP03 Generar el cierre del mes actual.")
    void rf45_cp03_mesEnCursoParcial() throws Exception {
        // ARRANGE
        YearMonth actual = YearMonth.from(Fechas.hoy());
        // ACT
        CierreMensual c = reporteService().cierreMensual(actual);
        // ASSERT
        assertTrue(c.parcial());
    }

    /** RF45-CP04 (E4). Mensaje «El mes aún no empieza». */
    @Test
    @DisplayName("RF45-CP04 Generar el cierre del próximo mes.")
    void rf45_cp04_mesFuturo() throws Exception {
        // ARRANGE
        YearMonth siguiente = YearMonth.from(Fechas.hoy()).plusMonths(1);
        // ACT
        NegocioException e = assertThrows(NegocioException.class, () -> reporteService().cierreMensual(siguiente));
        // ASSERT
        assertEquals("El mes aún no empieza", e.getMessage());
    }
}
