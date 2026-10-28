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
 * RF11 Registrar compra del día — pruebas TDD de la clase CompraServiceImpl.
 * Qué decide: cambia estado. Un método por caso de prueba (CP) de la ficha del Anexo B.
 */
@DisplayName("RF11 Registrar compra del día")
class RF11RegistrarCompraDelDiaTest extends TddBase {

    /** RF11-CP01 (E1). Se crea el lote de 40.00 kg a S/ 26.00 por kg y se recalcula el costo de la corvina. */
    @Test
    @DisplayName("RF11-CP01 Comprar 40 kg de corvina por S/ 1,040 al contado.")
    void rf11_cp01_compraCreaLote() throws Exception {
        // ARRANGE
        when(proveedorDao.buscar(1)).thenReturn(Optional.of(new Proveedor(1, "Distribuidora Pesquera Lurín", null, "PRINCIPAL", null)));
        when(productoDao.buscar(2)).thenReturn(Optional.of(producto(2, "Corvina")));
        when(compraDao.insertarCompra(anyInt(), anyInt(), any(), anyString())).thenReturn(9);
        when(compraDao.insertarDetalle(anyInt(), any())).thenReturn(15);
        // ACT
        compraService().registrarCompra(1, "CONTADO", List.of(new ItemCompra(2, d("40"), d("1040"))), 2);
        // ASSERT
        verify(loteDao).insertar(2, 15, Fechas.hoy(), d("40.00"), d("26.00"));
        verify(precioService).recalcularCosto(2);
    }

    /** RF11-CP02 (E2). La compra se guarda con tipo de pago CREDITO. */
    @Test
    @DisplayName("RF11-CP02 Comprar 20 kg de jurel por S/ 100 a crédito.")
    void rf11_cp02_compraCredito() throws Exception {
        // ARRANGE
        when(proveedorDao.buscar(2)).thenReturn(Optional.of(new Proveedor(2, "Don Pedro (terminal)", null, "OCASIONAL", null)));
        when(productoDao.buscar(5)).thenReturn(Optional.of(producto(5, "Jurel")));
        // ACT
        compraService().registrarCompra(2, "CREDITO", List.of(new ItemCompra(5, d("20"), d("100"))), 2);
        // ASSERT
        verify(compraDao).insertarCompra(2, 2, Fechas.hoy(), "CREDITO");
    }

    /** RF11-CP03 (E3). Mensaje «Los kilos y el costo deben ser mayores que cero»; no se crea la compra. */
    @Test
    @DisplayName("RF11-CP03 Comprar 0 kg de corvina por S/ 100.")
    void rf11_cp03_kilosCero() throws Exception {
        // ARRANGE
        when(proveedorDao.buscar(1)).thenReturn(Optional.of(new Proveedor(1, "Distribuidora Pesquera Lurín", null, "PRINCIPAL", null)));
        when(productoDao.buscar(2)).thenReturn(Optional.of(producto(2, "Corvina")));
        // ACT
        NegocioException e = assertThrows(NegocioException.class,
                () -> compraService().registrarCompra(1, "CONTADO", List.of(new ItemCompra(2, d("0"), d("100"))), 2));
        // ASSERT
        assertEquals("Los kilos y el costo deben ser mayores que cero", e.getMessage());
        verify(compraDao, never()).insertarCompra(anyInt(), anyInt(), any(), anyString());
    }

    /** RF11-CP04 (E4). Mensaje «Lenguado está inactivo». */
    @Test
    @DisplayName("RF11-CP04 Comprar 10 kg de lenguado.")
    void rf11_cp04_productoInactivo() throws Exception {
        // ARRANGE
        when(proveedorDao.buscar(1)).thenReturn(Optional.of(new Proveedor(1, "Distribuidora Pesquera Lurín", null, "PRINCIPAL", null)));
        when(productoDao.buscar(6)).thenReturn(Optional.of(new Producto(6, "Lenguado", "Lenguado", "PESCADO", "INACTIVO")));
        // ACT
        NegocioException e = assertThrows(NegocioException.class,
                () -> compraService().registrarCompra(1, "CONTADO", List.of(new ItemCompra(6, d("10"), d("400"))), 2));
        // ASSERT
        assertEquals("Lenguado está inactivo", e.getMessage());
    }
}
