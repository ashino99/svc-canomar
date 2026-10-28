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
 * RF18 Registrar venta al crédito — pruebas TDD de la clase VentaServiceImpl.
 * Qué decide: valida. Un método por caso de prueba (CP) de la ficha del Anexo B.
 */
@DisplayName("RF18 Registrar venta al crédito")
class RF18RegistrarVentaAlCreditoTest extends TddBase {

    /** RF18-CP01 (E1). Se guarda la venta CREDITO del cliente 1 por S/ 320.00, sin medio de pago. */
    @Test
    @DisplayName("RF18-CP01 Vender 10 kg de corvina al crédito.")
    void rf18_cp01_ventaCredito() throws Exception {
        // ARRANGE
        prepararCorvina();
        when(clienteDao.buscar(1)).thenReturn(Optional.of(cliente(1, "Cevichería El Muelle", "1500.00", 15)));
        when(credito.saldo(1)).thenReturn(saldoCliente(1, "228.00", "1500.00", "0.00"));
        // ACT
        ventaService().registrar(ventaCredito(1, item(2, "10")), 2, false, false);
        // ASSERT
        ArgumentCaptor<NuevaVenta> v = ArgumentCaptor.forClass(NuevaVenta.class);
        verify(ventaDao).insertarVenta(v.capture(), eq(2), isNull());
        assertEquals(1, v.getValue().idCliente());
        assertNull(v.getValue().medioPago());
        assertEquals(d("320.00"), v.getValue().total());
    }

    /** RF18-CP02 (E2). Pide confirmar: «Restaurante Sabor Norteño tiene deuda vencida de S/ 1372.00. ¿Registrar igual?». */
    @Test
    @DisplayName("RF18-CP02 Vender 2 kg de corvina al crédito sin confirmar.")
    void rf18_cp02_clienteConVencido() throws Exception {
        // ARRANGE
        prepararCorvina();
        when(clienteDao.buscar(2)).thenReturn(Optional.of(cliente(2, "Restaurante Sabor Norteño", "1500.00", 7)));
        when(credito.saldo(2)).thenReturn(saldoCliente(2, "1372.00", "1500.00", "1372.00"));
        // ACT
        ConfirmacionException e = assertThrows(ConfirmacionException.class,
                () -> ventaService().registrar(ventaCredito(2, item(2, "2")), 2, false, false));
        // ASSERT
        assertEquals("Restaurante Sabor Norteño tiene deuda vencida de S/ 1372.00. ¿Registrar igual?", e.getMessage());
    }

    /** RF18-CP03 (E3). Mensaje «Supera el límite de crédito: disponible S/ 128.00 de S/ 1500.00. Requiere autorización del administrador». */
    @Test
    @DisplayName("RF18-CP03 El vendedor vende 10 kg de corvina (S/ 320) al crédito.")
    void rf18_cp03_superaLimiteVendedor() throws Exception {
        // ARRANGE
        prepararCorvina();
        when(clienteDao.buscar(2)).thenReturn(Optional.of(cliente(2, "Restaurante Sabor Norteño", "1500.00", 7)));
        when(credito.saldo(2)).thenReturn(saldoCliente(2, "1372.00", "1500.00", "0.00"));
        // ACT
        NegocioException e = assertThrows(NegocioException.class,
                () -> ventaService().registrar(ventaCredito(2, item(2, "10")), 2, false, true));
        // ASSERT
        assertEquals("Supera el límite de crédito: disponible S/ 128.00 de S/ 1500.00. Requiere autorización del administrador", e.getMessage());
        verify(ventaDao, never()).insertarVenta(any(), anyInt(), any());
    }

    /** RF18-CP04 (E4). Mensaje «Este cliente solo puede comprar al contado». */
    @Test
    @DisplayName("RF18-CP04 Vender al crédito a la pollería.")
    void rf18_cp04_sinCreditoHabilitado() throws Exception {
        // ARRANGE
        prepararCorvina();
        when(clienteDao.buscar(3)).thenReturn(Optional.of(cliente(3, "Pollería Don Lucho", "0.00", 7)));
        // ACT
        NegocioException e = assertThrows(NegocioException.class,
                () -> ventaService().registrar(ventaCredito(3, item(2, "2")), 2, false, false));
        // ASSERT
        assertEquals("Este cliente solo puede comprar al contado", e.getMessage());
    }
}
