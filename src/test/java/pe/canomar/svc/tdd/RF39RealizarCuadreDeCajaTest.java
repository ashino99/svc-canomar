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
 * RF39 Realizar cuadre de caja — pruebas TDD de la clase CajaServiceImpl.
 * Qué decide: calcula. Un método por caso de prueba (CP) de la ficha del Anexo B.
 */
@DisplayName("RF39 Realizar cuadre de caja")
class RF39RealizarCuadreDeCajaTest extends TddBase {

    /** RF39-CP01 (E1). Efectivo esperado S/ 520.00, diferencia S/ 0.00; se guarda el cuadre. */
    @Test
    @DisplayName("RF39-CP01 Cuadrar con S/ 520 contados y S/ 200 revisados.")
    void rf39_cp01_cuadreCoincide() throws Exception {
        // ARRANGE
        LocalDate hoy = Fechas.hoy();
        prepararMovimientos(hoy, "450.00", "200.00", "100.00", "0.00", "30.00", "0.00");
        when(cuadreDao.buscar(hoy)).thenReturn(Optional.empty(), Optional.empty(), Optional.of(cuadreVista(hoy)));
        // ACT
        cajaService().realizarCuadre(hoy, d("520"), d("200"), null, 1);
        // ASSERT
        verify(cuadreDao).insertar(eq(1), eq(hoy), eq(d("520.00")), eq(d("520.00")), eq(d("200.00")), eq(d("200.00")), isNull(), any());
    }

    /** RF39-CP02 (E2). Se guarda: S/ -3.00 está dentro de la tolerancia. */
    @Test
    @DisplayName("RF39-CP02 Cuadrar con S/ 517 sin motivo.")
    void rf39_cp02_diferenciaMenorTolerancia() throws Exception {
        // ARRANGE
        LocalDate hoy = Fechas.hoy();
        prepararMovimientos(hoy, "450.00", "200.00", "100.00", "0.00", "30.00", "0.00");
        when(cuadreDao.buscar(hoy)).thenReturn(Optional.empty(), Optional.empty(), Optional.of(cuadreVista(hoy)));
        // ACT
        cajaService().realizarCuadre(hoy, d("517"), d("200"), "", 1);
        // ASSERT
        verify(cuadreDao).insertar(eq(1), eq(hoy), eq(d("520.00")), eq(d("517.00")), any(), any(), isNull(), any());
    }

    /** RF39-CP03 (E3). Mensaje «Diferencia de S/ -20.00 en efectivo y S/ 0.00 en digital: indique el motivo». */
    @Test
    @DisplayName("RF39-CP03 Cuadrar con S/ 500 sin motivo.")
    void rf39_cp03_diferenciaExigeMotivo() throws Exception {
        // ARRANGE
        LocalDate hoy = Fechas.hoy();
        prepararMovimientos(hoy, "450.00", "200.00", "100.00", "0.00", "30.00", "0.00");
        // ACT
        NegocioException e = assertThrows(NegocioException.class, () -> cajaService().realizarCuadre(hoy, d("500"), d("200"), null, 1));
        // ASSERT
        assertEquals("Diferencia de S/ -20.00 en efectivo y S/ 0.00 en digital: indique el motivo", e.getMessage());
    }

    /** RF39-CP04 (E4). Mensaje «El día 27/10/2026 ya tiene cuadre». */
    @Test
    @DisplayName("RF39-CP04 Cuadrar otra vez el 27/10/2026.")
    void rf39_cp04_diaYaCuadrado() throws Exception {
        // ARRANGE
        LocalDate dia = LocalDate.of(2026, 10, 27);
        when(cuadreDao.buscar(dia)).thenReturn(Optional.of(cuadreVista(dia)));
        // ACT
        NegocioException e = assertThrows(NegocioException.class, () -> cajaService().realizarCuadre(dia, d("100"), d("0"), null, 1));
        // ASSERT
        assertEquals("El día 27/10/2026 ya tiene cuadre", e.getMessage());
    }
}
