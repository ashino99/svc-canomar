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
 * RF15 Registrar precios de venta del día — pruebas TDD de la clase PrecioServiceImpl.
 * Qué decide: valida. Un método por caso de prueba (CP) de la ficha del Anexo B.
 */
@DisplayName("RF15 Registrar precios de venta del día")
class RF15RegistrarPreciosDeVentaDelDiaTest extends TddBase {

    /** RF15-CP01 (E1). Se guarda 1 precio para hoy. */
    @Test
    @DisplayName("RF15-CP01 Guardar precio de corvina S/ 32.00.")
    void rf15_cp01_guardaPrecios() throws Exception {
        // ARRANGE
        when(precioDao.preciosDe(any())).thenReturn(List.of(precioVista(2, "Corvina", "26.00", null, "32.00")));
        // ACT
        int n = precioServiceReal().guardarPrecios(Map.of(2, d("32")), false, 2);
        // ASSERT
        assertEquals(1, n);
        verify(precioDao).guardarPrecio(2, Fechas.hoy(), d("32.00"), d("26.00"));
    }

    /** RF15-CP02 (E2). Se copia S/ 9.00 como precio de hoy del bonito. */
    @Test
    @DisplayName("RF15-CP02 Tocar «Usar precios de ayer».")
    void rf15_cp02_usarPreciosDeAyer() throws Exception {
        // ARRANGE
        when(precioDao.preciosDe(any())).thenReturn(List.of(precioVista(1, "Bonito", "6.50", null, "9.00")));
        // ACT
        int n = precioServiceReal().usarPreciosDeAyer(2);
        // ASSERT
        assertEquals(1, n);
        verify(precioDao).guardarPrecio(1, Fechas.hoy(), d("9.00"), d("6.50"));
    }

    /** RF15-CP03 (E3). Pide confirmar: «Corvina: S/ 24.00 está por debajo del costo S/ 26.00. ¿Confirmar los precios?». */
    @Test
    @DisplayName("RF15-CP03 Guardar corvina a S/ 24.00 sin confirmar.")
    void rf15_cp03_precioBajoCosto() throws Exception {
        // ARRANGE
        when(precioDao.preciosDe(any())).thenReturn(List.of(precioVista(2, "Corvina", "26.00", null, "32.00")));
        // ACT
        ConfirmacionException e = assertThrows(ConfirmacionException.class,
                () -> precioServiceReal().guardarPrecios(Map.of(2, d("24")), false, 2));
        // ASSERT
        assertEquals("Corvina: S/ 24.00 está por debajo del costo S/ 26.00. ¿Confirmar los precios?", e.getMessage());
        verify(precioDao, never()).guardarPrecio(anyInt(), any(), any(), any());
    }

    /** RF15-CP04 (E4). Mensaje «Jurel: el precio debe ser mayor que cero». */
    @Test
    @DisplayName("RF15-CP04 Guardar jurel a S/ 0.")
    void rf15_cp04_precioCero() throws Exception {
        // ARRANGE
        when(precioDao.preciosDe(any())).thenReturn(List.of(precioVista(5, "Jurel", "5.00", null, "8.00")));
        // ACT
        NegocioException e = assertThrows(NegocioException.class,
                () -> precioServiceReal().guardarPrecios(Map.of(5, d("0")), false, 2));
        // ASSERT
        assertEquals("Jurel: el precio debe ser mayor que cero", e.getMessage());
    }
}
