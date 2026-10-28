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
 * RF21 Registrar venta de remate — pruebas TDD de la clase VentaServiceImpl.
 * Qué decide: valida. Un método por caso de prueba (CP) de la ficha del Anexo B.
 */
@DisplayName("RF21 Registrar venta de remate")
class RF21RegistrarVentaDeRemateTest extends TddBase {

    /** RF21-CP01 (E1). Se guarda la venta marcada como remate con detalle a S/ 6.00 por kg. */
    @Test
    @DisplayName("RF21-CP01 Rematar 4 kg a S/ 6.00 al contado.")
    void rf21_cp01_registraRemate() throws Exception {
        // ARRANGE
        prepararBonito();
        // ACT
        ventaService().registrar(remate(null, item(1, "4", "6.00")), 2, false, false);
        // ASSERT
        ArgumentCaptor<NuevaVenta> v = ArgumentCaptor.forClass(NuevaVenta.class);
        verify(ventaDao).insertarVenta(v.capture(), eq(2), isNull());
        assertTrue(v.getValue().esRemate());
        verify(ventaDao).insertarDetalle(7L, 1, d("4.00"), d("6.00"), d("6.50"));
    }

    /** RF21-CP02 (E2). Se guarda la venta CREDITO del cliente 1 marcada como remate. */
    @Test
    @DisplayName("RF21-CP02 Rematar 4 kg de bonito a S/ 6.00 al crédito del cliente 1.")
    void rf21_cp02_remateAlCredito() throws Exception {
        // ARRANGE
        prepararBonito();
        when(clienteDao.buscar(1)).thenReturn(Optional.of(cliente(1, "Cevichería El Muelle", "1500.00", 15)));
        when(credito.saldo(1)).thenReturn(saldoCliente(1, "228.00", "1500.00", "0.00"));
        // ACT
        ventaService().registrar(remate(1, item(1, "4", "6.00")), 2, false, false);
        // ASSERT
        ArgumentCaptor<NuevaVenta> v = ArgumentCaptor.forClass(NuevaVenta.class);
        verify(ventaDao).insertarVenta(v.capture(), eq(2), isNull());
        assertEquals("CREDITO", v.getValue().tipoPago());
        assertTrue(v.getValue().esRemate());
    }

    /** RF21-CP03 (E3). Mensaje «Bonito: el precio de remate debe ser menor al del día (S/ 9.00)». */
    @Test
    @DisplayName("RF21-CP03 Rematar a S/ 9.50.")
    void rf21_cp03_remateNoMenor() throws Exception {
        // ARRANGE
        prepararBonito();
        // ACT
        NegocioException e = assertThrows(NegocioException.class,
                () -> ventaService().registrar(remate(null, item(1, "4", "9.50")), 2, false, false));
        // ASSERT
        assertEquals("Bonito: el precio de remate debe ser menor al del día (S/ 9.00)", e.getMessage());
    }

    /** RF21-CP04 (E4). Mensaje «Perico: ingrese el precio de hoy de este producto». */
    @Test
    @DisplayName("RF21-CP04 Rematar perico sin escribir precio.")
    void rf21_cp04_remateSinPrecio() throws Exception {
        // ARRANGE
        when(productoDao.buscar(7)).thenReturn(Optional.of(producto(7, "Perico")));
        // ACT
        NegocioException e = assertThrows(NegocioException.class,
                () -> ventaService().registrar(remate(null, item(7, "3")), 2, false, false));
        // ASSERT
        assertEquals("Perico: ingrese el precio de hoy de este producto", e.getMessage());
    }
}
