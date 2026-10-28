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
 * RF07 Editar producto — pruebas TDD de la clase ProductoServiceImpl.
 * Qué decide: cambia estado. Un método por caso de prueba (CP) de la ficha del Anexo B.
 */
@DisplayName("RF07 Editar producto")
class RF07EditarProductoTest extends TddBase {

    /** RF07-CP01 (E1). Se actualiza el producto 3 y se registra EDITAR_PRODUCTO. */
    @Test
    @DisplayName("RF07-CP01 Cambiar el nombre corto a «Pota fresca».")
    void rf07_cp01_editaProducto() throws Exception {
        // ARRANGE
        when(productoDao.buscar(3)).thenReturn(Optional.of(new Producto(3, "Pota", "Pota", "MOLUSCO", "ACTIVO")));
        // ACT
        productoService().editar(3, "Pota", "Pota fresca", "MOLUSCO", "ACTIVO", 1);
        // ASSERT
        verify(productoDao).actualizar(new Producto(3, "Pota", "Pota fresca", "MOLUSCO", "ACTIVO"));
        verify(auditoria).registrar(1, "EDITAR_PRODUCTO", "Pota · ACTIVO");
    }

    /** RF07-CP02 (E2). El producto queda INACTIVO. */
    @Test
    @DisplayName("RF07-CP02 Cambiar su estado a INACTIVO.")
    void rf07_cp02_inactivaSinSaldo() throws Exception {
        // ARRANGE
        when(productoDao.buscar(6)).thenReturn(Optional.of(new Producto(6, "Lenguado", "Lenguado", "PESCADO", "ACTIVO")));
        when(loteDao.kilosDisponibles(6)).thenReturn(d("0.00"));
        // ACT
        productoService().editar(6, "Lenguado", "Lenguado", "PESCADO", "INACTIVO", 1);
        // ASSERT
        verify(productoDao).actualizar(new Producto(6, "Lenguado", "Lenguado", "PESCADO", "INACTIVO"));
    }

    /** RF07-CP03 (E3). Mensaje «No se puede inactivar: tiene 8.00 kg en saldo». */
    @Test
    @DisplayName("RF07-CP03 Cambiar su estado a INACTIVO.")
    void rf07_cp03_noInactivaConSaldo() throws Exception {
        // ARRANGE
        when(productoDao.buscar(1)).thenReturn(Optional.of(producto(1, "Bonito")));
        when(loteDao.kilosDisponibles(1)).thenReturn(d("8.00"));
        // ACT
        NegocioException e = assertThrows(NegocioException.class,
                () -> productoService().editar(1, "Bonito", "Bonito", "PESCADO", "INACTIVO", 1));
        // ASSERT
        assertEquals("No se puede inactivar: tiene 8.00 kg en saldo", e.getMessage());
        verify(productoDao, never()).actualizar(any());
    }

    /** RF07-CP04 (E4). Mensaje «Ya existe otro producto con ese nombre». */
    @Test
    @DisplayName("RF07-CP04 Renombrar el producto 5 como «Pota».")
    void rf07_cp04_nombreDuplicado() throws Exception {
        // ARRANGE
        when(productoDao.buscar(5)).thenReturn(Optional.of(producto(5, "Jurel")));
        when(productoDao.existeNombre("Pota", 5)).thenReturn(true);
        // ACT
        NegocioException e = assertThrows(NegocioException.class,
                () -> productoService().editar(5, "Pota", "Pota", "PESCADO", "ACTIVO", 1));
        // ASSERT
        assertEquals("Ya existe otro producto con ese nombre", e.getMessage());
    }
}
