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
 * RF10 Registrar proveedor — pruebas TDD de la clase CompraServiceImpl.
 * Qué decide: valida. Un método por caso de prueba (CP) de la ficha del Anexo B.
 */
@DisplayName("RF10 Registrar proveedor")
class RF10RegistrarProveedorTest extends TddBase {

    /** RF10-CP01 (E1). Se inserta el proveedor PRINCIPAL. */
    @Test
    @DisplayName("RF10-CP01 Registrar «Pesquera San José», RUC 20509876543, PRINCIPAL.")
    void rf10_cp01_registraProveedor() throws Exception {
        // ARRANGE
        when(proveedorDao.insertar(any())).thenReturn(3);
        // ACT
        int id = compraService().registrarProveedor("Pesquera San José", "20509876543", "PRINCIPAL", "999000111", 1);
        // ASSERT
        assertEquals(3, id);
        verify(proveedorDao).insertar(new Proveedor(null, "Pesquera San José", "20509876543", "PRINCIPAL", "999000111"));
    }

    /** RF10-CP02 (E2). Se inserta como OCASIONAL, sin RUC ni teléfono. */
    @Test
    @DisplayName("RF10-CP02 Registrar solo el nombre «Doña Juana».")
    void rf10_cp02_ocasionalSoloNombre() throws Exception {
        // ARRANGE
        // ACT
        compraService().registrarProveedor("Doña Juana", null, null, null, 1);
        // ASSERT
        verify(proveedorDao).insertar(new Proveedor(null, "Doña Juana", null, "OCASIONAL", null));
    }

    /** RF10-CP03 (E3). Mensaje «El proveedor ya está registrado». */
    @Test
    @DisplayName("RF10-CP03 Registrar «Don Pedro (terminal)».")
    void rf10_cp03_proveedorDuplicado() throws Exception {
        // ARRANGE
        when(proveedorDao.existeNombre("Don Pedro (terminal)")).thenReturn(true);
        // ACT
        NegocioException e = assertThrows(NegocioException.class,
                () -> compraService().registrarProveedor("Don Pedro (terminal)", null, "OCASIONAL", null, 1));
        // ASSERT
        assertEquals("El proveedor ya está registrado", e.getMessage());
    }

    /** RF10-CP04 (E4). Mensaje «El RUC debe tener 11 dígitos». */
    @Test
    @DisplayName("RF10-CP04 Registrar con RUC «12345».")
    void rf10_cp04_rucInvalido() throws Exception {
        // ARRANGE
        // ACT
        NegocioException e = assertThrows(NegocioException.class,
                () -> compraService().registrarProveedor("Pesquera San José", "12345", "PRINCIPAL", null, 1));
        // ASSERT
        assertEquals("El RUC debe tener 11 dígitos", e.getMessage());
    }
}
