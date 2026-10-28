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
 * RF13 Registrar pago a proveedor — pruebas TDD de la clase CompraServiceImpl.
 * Qué decide: cambia estado. Un método por caso de prueba (CP) de la ficha del Anexo B.
 */
@DisplayName("RF13 Registrar pago a proveedor")
class RF13RegistrarPagoAProveedorTest extends TddBase {

    /** RF13-CP01 (E1). Se registra el pago y la deuda queda en S/ 0.00. */
    @Test
    @DisplayName("RF13-CP01 Pagar S/ 390 por transferencia.")
    void rf13_cp01_pagoTotal() throws Exception {
        // ARRANGE
        when(compraDao.deudas()).thenReturn(List.of(deudaProveedor(1, "Distribuidora Pesquera Lurín", "390.00", "0.00")));
        // ACT
        BigDecimal queda = compraService().registrarPago(1, d("390"), "TRANSFERENCIA", 1);
        // ASSERT
        assertEquals(d("0.00"), queda);
        verify(compraDao).insertarPago(1, 1, Fechas.hoy(), d("390.00"), "TRANSFERENCIA");
    }

    /** RF13-CP02 (E2). La deuda queda en S/ 60.00. */
    @Test
    @DisplayName("RF13-CP02 Pagar S/ 40 en efectivo.")
    void rf13_cp02_pagoParcial() throws Exception {
        // ARRANGE
        when(compraDao.deudas()).thenReturn(List.of(deudaProveedor(2, "Don Pedro (terminal)", "100.00", "0.00")));
        // ACT
        BigDecimal queda = compraService().registrarPago(2, d("40"), "EFECTIVO", 1);
        // ASSERT
        assertEquals(d("60.00"), queda);
    }

    /** RF13-CP03 (E3). Mensaje «El pago supera la deuda (S/ 100.00)». */
    @Test
    @DisplayName("RF13-CP03 Pagar S/ 150.")
    void rf13_cp03_pagoSuperaDeuda() throws Exception {
        // ARRANGE
        when(compraDao.deudas()).thenReturn(List.of(deudaProveedor(2, "Don Pedro (terminal)", "100.00", "0.00")));
        // ACT
        NegocioException e = assertThrows(NegocioException.class, () -> compraService().registrarPago(2, d("150"), "EFECTIVO", 1));
        // ASSERT
        assertEquals("El pago supera la deuda (S/ 100.00)", e.getMessage());
    }

    /** RF13-CP04 (E4). Mensaje «Ingrese un monto mayor que cero». */
    @Test
    @DisplayName("RF13-CP04 Pagar S/ 0.")
    void rf13_cp04_montoCero() throws Exception {
        // ARRANGE
        // ACT
        NegocioException e = assertThrows(NegocioException.class, () -> compraService().registrarPago(2, d("0"), "EFECTIVO", 1));
        // ASSERT
        assertEquals("Ingrese un monto mayor que cero", e.getMessage());
        verify(compraDao, never()).insertarPago(anyInt(), anyInt(), any(), any(), anyString());
    }
}
