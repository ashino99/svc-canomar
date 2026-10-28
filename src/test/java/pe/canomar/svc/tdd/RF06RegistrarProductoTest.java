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
 * RF06 Registrar producto — pruebas TDD de la clase ProductoServiceImpl.
 * Qué decide: valida. Un método por caso de prueba (CP) de la ficha del Anexo B.
 */
@DisplayName("RF06 Registrar producto")
class RF06RegistrarProductoTest extends TddBase {

    /** RF06-CP01 (E1). Se inserta «Caballa» ACTIVO (nombre con mayúscula inicial). */
    @Test
    @DisplayName("RF06-CP01 Registrar «caballa», nombre corto «Caballa», categoría PESCADO.")
    void rf06_cp01_registraProducto() throws Exception {
        // ARRANGE
        when(productoDao.existeNombre("Caballa", null)).thenReturn(false);
        when(productoDao.insertar(any())).thenReturn(8);
        // ACT
        int id = productoService().registrar("caballa", "Caballa", "PESCADO", 1);
        // ASSERT
        assertEquals(8, id);
        verify(productoDao).insertar(new Producto(null, "Caballa", "Caballa", "PESCADO", "ACTIVO"));
    }

    /** RF06-CP02 (E2). El nombre corto queda «Concha de abanico» (máx. 20 caracteres). */
    @Test
    @DisplayName("RF06-CP02 Registrar sin nombre corto.")
    void rf06_cp02_nombreCortoPorDefecto() throws Exception {
        // ARRANGE
        when(productoDao.existeNombre("Concha de abanico", null)).thenReturn(false);
        // ACT
        productoService().registrar("concha de abanico", "", "MOLUSCO", 1);
        // ASSERT
        ArgumentCaptor<Producto> p = ArgumentCaptor.forClass(Producto.class);
        verify(productoDao).insertar(p.capture());
        assertEquals("Concha de abanico", p.getValue().nombreCorto());
    }

    /** RF06-CP03 (E3). Mensaje «El producto ya existe». */
    @Test
    @DisplayName("RF06-CP03 Registrar «Bonito».")
    void rf06_cp03_productoDuplicado() throws Exception {
        // ARRANGE
        when(productoDao.existeNombre("Bonito", null)).thenReturn(true);
        // ACT
        NegocioException e = assertThrows(NegocioException.class, () -> productoService().registrar("Bonito", null, "PESCADO", 1));
        // ASSERT
        assertEquals("El producto ya existe", e.getMessage());
    }

    /** RF06-CP04 (E4). Mensaje «Ingrese el nombre del producto». */
    @Test
    @DisplayName("RF06-CP04 Registrar con el nombre en blanco.")
    void rf06_cp04_faltaNombre() throws Exception {
        // ARRANGE
        // ACT
        NegocioException e = assertThrows(NegocioException.class, () -> productoService().registrar("   ", null, "PESCADO", 1));
        // ASSERT
        assertEquals("Ingrese el nombre del producto", e.getMessage());
    }
}
