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
 * RF36 Generar estado de cuenta — pruebas TDD de la clase CreditoServiceImpl.
 * Qué decide: calcula. Un método por caso de prueba (CP) de la ficha del Anexo B.
 */
@DisplayName("RF36 Generar estado de cuenta")
class RF36GenerarEstadoDeCuentaTest extends TddBase {

    /** RF36-CP01 (E1). Saldo inicial S/ 100.00, movimientos con saldo 328.00 y 278.00; saldo final S/ 278.00. */
    @Test
    @DisplayName("RF36-CP01 Generar del 01/10 al 31/10.")
    void rf36_cp01_saldoAcumulado() throws Exception {
        // ARRANGE
        LocalDate desde = LocalDate.of(2026, 10, 1), hasta = LocalDate.of(2026, 10, 31);
        when(clienteDao.buscar(1)).thenReturn(Optional.of(cliente(1, "Cevichería El Muelle", "1500.00", 15)));
        when(ventaDao.ventasCreditoAntesDe(eq(1), any())).thenReturn(d("100.00"));
        when(abonoDao.abonosAntesDe(eq(1), any())).thenReturn(d("0.00"));
        when(ventaDao.ventasCreditoCliente(eq(1), any(), any())).thenReturn(List.of(
                new VentaVista(3, "u3", LocalDateTime.of(2026, 10, 21, 8, 0), 1, "Cevichería El Muelle", "CREDITO", null, false, "VIGENTE", null, d("228.00"), "vendedor", null, false)));
        when(abonoDao.abonosCliente(eq(1), any(), any())).thenReturn(List.of(
                new AbonoVista(1, LocalDateTime.of(2026, 10, 25, 9, 0), "Cevichería El Muelle", d("50.00"), "YAPE", "VIGENTE")));
        // ACT
        EstadoCuenta ec = creditoService().estadoCuenta(1, desde, hasta);
        // ASSERT
        assertEquals(d("100.00"), ec.saldoInicial());
        assertEquals(d("328.00"), ec.movimientos().get(0).saldo());
        assertEquals(d("278.00"), ec.movimientos().get(1).saldo());
        assertEquals(d("278.00"), ec.saldoFinal());
    }

    /** RF36-CP02 (E2). Sin movimientos; saldo final = saldo inicial = S/ 0.00. */
    @Test
    @DisplayName("RF36-CP02 Generar octubre.")
    void rf36_cp02_sinMovimientos() throws Exception {
        // ARRANGE
        when(clienteDao.buscar(3)).thenReturn(Optional.of(cliente(3, "Pollería Don Lucho", "800.00", 7)));
        when(ventaDao.ventasCreditoAntesDe(eq(3), any())).thenReturn(d("0.00"));
        when(abonoDao.abonosAntesDe(eq(3), any())).thenReturn(d("0.00"));
        // ACT
        EstadoCuenta ec = creditoService().estadoCuenta(3, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
        // ASSERT
        assertTrue(ec.movimientos().isEmpty());
        assertEquals(ec.saldoInicial(), ec.saldoFinal());
    }

    /** RF36-CP03 (E3). Mensaje «Revise el rango de fechas». */
    @Test
    @DisplayName("RF36-CP03 Generar del 31/10 al 01/10.")
    void rf36_cp03_rangoInvertido() throws Exception {
        // ARRANGE
        when(clienteDao.buscar(1)).thenReturn(Optional.of(cliente(1, "Cevichería El Muelle", "1500.00", 15)));
        // ACT
        NegocioException e = assertThrows(NegocioException.class,
                () -> creditoService().estadoCuenta(1, LocalDate.of(2026, 10, 31), LocalDate.of(2026, 10, 1)));
        // ASSERT
        assertEquals("Revise el rango de fechas", e.getMessage());
    }

    /** RF36-CP04 (E4). Mensaje «Elija un periodo de hasta un año». */
    @Test
    @DisplayName("RF36-CP04 Generar del 01/01/2025 al 31/10/2026.")
    void rf36_cp04_periodoMayorAUnAnio() throws Exception {
        // ARRANGE
        when(clienteDao.buscar(1)).thenReturn(Optional.of(cliente(1, "Cevichería El Muelle", "1500.00", 15)));
        // ACT
        NegocioException e = assertThrows(NegocioException.class,
                () -> creditoService().estadoCuenta(1, LocalDate.of(2025, 1, 1), LocalDate.of(2026, 10, 31)));
        // ASSERT
        assertEquals("Elija un periodo de hasta un año", e.getMessage());
    }
}
