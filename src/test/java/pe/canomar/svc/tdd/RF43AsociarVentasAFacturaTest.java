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
 * RF43 Asociar ventas a factura — pruebas TDD de la clase FacturaServiceImpl.
 * Qué decide: valida. Un método por caso de prueba (CP) de la ficha del Anexo B.
 */
@DisplayName("RF43 Asociar ventas a factura")
class RF43AsociarVentasAFacturaTest extends TddBase {

    /** RF43-CP01 (E1). Se asigna la factura 1 a la venta 3; diferencia S/ 0.00. */
    @Test
    @DisplayName("RF43-CP01 Asociar la venta 3.")
    void rf43_cp01_asociaVentas() throws Exception {
        // ARRANGE
        prepararFactura(1, 1, "228.00");
        when(ventaDao.sinFacturaCliente(1)).thenReturn(List.of(venta(3, 1, "CREDITO", "228.00", "VIGENTE", null)));
        // ACT
        BigDecimal dif = facturaService().asociar(1, List.of(3L), false, 3);
        // ASSERT
        assertEquals(d("0.00"), dif);
        verify(ventaDao).asignarFactura(List.of(3L), 1);
    }

    /** RF43-CP02 (E2). Pide confirmar: «La suma de las ventas (S/ 200.00) difiere del importe pendiente de la factura en S/ 28.00. ¿Confirmar?». */
    @Test
    @DisplayName("RF43-CP02 Asociar sin confirmar.")
    void rf43_cp02_diferenciaPideConfirmar() throws Exception {
        // ARRANGE
        prepararFactura(1, 1, "228.00");
        when(ventaDao.sinFacturaCliente(1)).thenReturn(List.of(venta(3, 1, "CREDITO", "200.00", "VIGENTE", null)));
        // ACT
        ConfirmacionException e = assertThrows(ConfirmacionException.class, () -> facturaService().asociar(1, List.of(3L), false, 3));
        // ASSERT
        assertEquals("La suma de las ventas (S/ 200.00) difiere del importe pendiente de la factura en S/ 28.00. ¿Confirmar?", e.getMessage());
        verify(ventaDao, never()).asignarFactura(any(), anyInt());
    }

    /** RF43-CP03 (E3). Mensaje «La venta 0001 ya está asociada a otra factura o no es de este cliente». */
    @Test
    @DisplayName("RF43-CP03 Asociar la venta 1 a la factura 1.")
    void rf43_cp03_ventaNoDisponible() throws Exception {
        // ARRANGE
        prepararFactura(1, 1, "228.00");
        when(ventaDao.sinFacturaCliente(1)).thenReturn(List.of());
        // ACT
        NegocioException e = assertThrows(NegocioException.class, () -> facturaService().asociar(1, List.of(1L), false, 3));
        // ASSERT
        assertEquals("La venta 0001 ya está asociada a otra factura o no es de este cliente", e.getMessage());
    }

    /** RF43-CP04 (E4). Mensaje «Seleccione las ventas que sustenta la factura». */
    @Test
    @DisplayName("RF43-CP04 Asociar sin marcar ventas.")
    void rf43_cp04_sinVentas() throws Exception {
        // ARRANGE
        prepararFactura(1, 1, "228.00");
        // ACT
        NegocioException e = assertThrows(NegocioException.class, () -> facturaService().asociar(1, List.of(), false, 3));
        // ASSERT
        assertEquals("Seleccione las ventas que sustenta la factura", e.getMessage());
    }
}
