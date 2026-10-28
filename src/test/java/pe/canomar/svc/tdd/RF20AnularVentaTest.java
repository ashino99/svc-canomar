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
 * RF20 Anular venta — pruebas TDD de la clase VentaServiceImpl.
 * Qué decide: cambia estado. Un método por caso de prueba (CP) de la ficha del Anexo B.
 */
@DisplayName("RF20 Anular venta")
class RF20AnularVentaTest extends TddBase {

    /** RF20-CP01 (E1). Se devuelven 1.85 kg al lote 10 y la venta queda anulada. */
    @Test
    @DisplayName("RF20-CP01 Anular con motivo «Error de peso».")
    void rf20_cp01_anulaYDevuelveKilos() throws Exception {
        // ARRANGE
        when(ventaDao.buscar(7L)).thenReturn(Optional.of(venta(7, null, "CONTADO", "59.20", "VIGENTE", null)));
        when(ventaDao.detalle(7L)).thenReturn(List.of(new DetalleVentaVista(2, "Corvina", d("1.85"), d("32.00"), d("26.00"))));
        when(loteDao.ultimoLote(2)).thenReturn(Optional.of(10));
        // ACT
        ventaService().anular(7L, "Error de peso", 1);
        // ASSERT
        verify(loteDao).sumar(10, d("1.85"));
        verify(ventaDao).anular(7L, "Error de peso");
    }

    /** RF20-CP02 (E2). La bitácora termina en «tenía factura: emitir nota de crédito en SUNAT». */
    @Test
    @DisplayName("RF20-CP02 Anular la venta 8.")
    void rf20_cp02_ventaConFactura() throws Exception {
        // ARRANGE
        when(ventaDao.buscar(8L)).thenReturn(Optional.of(venta(8, null, "CONTADO", "96.00", "VIGENTE", 3)));
        // ACT
        ventaService().anular(8L, "Cliente devolvió", 1);
        // ASSERT
        ArgumentCaptor<String> det = ArgumentCaptor.forClass(String.class);
        verify(auditoria).registrar(eq(1), eq("ANULAR_VENTA"), det.capture());
        assertTrue(det.getValue().endsWith("tenía factura: emitir nota de crédito en SUNAT"));
    }

    /** RF20-CP03 (E3). Mensaje «La venta ya tiene abonos aplicados (S/ 50.00); no se puede anular». */
    @Test
    @DisplayName("RF20-CP03 Anular la venta 3.")
    void rf20_cp03_conAbonos() throws Exception {
        // ARRANGE
        when(ventaDao.buscar(3L)).thenReturn(Optional.of(venta(3, 1, "CREDITO", "228.00", "VIGENTE", null)));
        when(credito.pendientes(1)).thenReturn(List.of(new PendienteVenta(3L, Fechas.ahora().minusDays(7), d("228.00"), d("50.00"))));
        // ACT
        NegocioException e = assertThrows(NegocioException.class, () -> ventaService().anular(3L, "Error", 1));
        // ASSERT
        assertEquals("La venta ya tiene abonos aplicados (S/ 50.00); no se puede anular", e.getMessage());
        verify(ventaDao, never()).anular(anyLong(), anyString());
    }

    /** RF20-CP04 (E4). Mensaje «Indique el motivo de la anulación». */
    @Test
    @DisplayName("RF20-CP04 Anular sin escribir motivo.")
    void rf20_cp04_faltaMotivo() throws Exception {
        // ARRANGE
        when(ventaDao.buscar(7L)).thenReturn(Optional.of(venta(7, null, "CONTADO", "59.20", "VIGENTE", null)));
        // ACT
        NegocioException e = assertThrows(NegocioException.class, () -> ventaService().anular(7L, "  ", 1));
        // ASSERT
        assertEquals("Indique el motivo de la anulación", e.getMessage());
    }
}
