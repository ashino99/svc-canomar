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
 * RF40 Calcular ganancia del día — pruebas TDD de la clase Calculos / CajaServiceImpl.
 * Qué decide: calcula. Un método por caso de prueba (CP) de la ficha del Anexo B.
 */
@DisplayName("RF40 Calcular ganancia del día")
class RF40CalcularGananciaDelDiaTest extends TddBase {

    /** RF40-CP01 (E1). Ganancia S/ 305.00. */
    @Test
    @DisplayName("RF40-CP01 Calcular la ganancia.")
    void rf40_cp01_formulaGanancia() throws Exception {
        // ARRANGE
        BigDecimal ventas = d("1200.00"), costo = d("850.00"), perdidas = d("0.00"), gastos = d("45.00");
        // ACT
        BigDecimal g = Calculos.ganancia(ventas, costo, perdidas, gastos);
        // ASSERT
        assertEquals(d("305.00"), g);
    }

    /** RF40-CP02 (E2). Ganancia S/ 279.00 y margen 23.3 %. */
    @Test
    @DisplayName("RF40-CP02 Calcular la ganancia del día.")
    void rf40_cp02_perdidasRestan() throws Exception {
        // ARRANGE
        LocalDate hoy = Fechas.hoy();
        when(ventaDao.cantidadVentas(hoy, hoy)).thenReturn(18);
        when(ventaDao.totalVentas(hoy, hoy)).thenReturn(d("1200.00"));
        when(ventaDao.costoVendido(hoy, hoy)).thenReturn(d("850.00"));
        when(loteDao.perdidas(hoy, hoy)).thenReturn(d("26.00"));
        when(gastoDao.total(hoy, hoy)).thenReturn(d("45.00"));
        // ACT
        Ganancia g = cajaService().ganancia(hoy);
        // ASSERT
        assertEquals(d("279.00"), g.ganancia());
        assertEquals(new BigDecimal("23.3"), g.margen());
    }

    /** RF40-CP03 (E3). Resultado marcado como provisional. */
    @Test
    @DisplayName("RF40-CP03 Calcular la ganancia de hoy.")
    void rf40_cp03_provisionalSinCuadre() throws Exception {
        // ARRANGE
        LocalDate hoy = Fechas.hoy();
        when(cuadreDao.buscar(hoy)).thenReturn(Optional.empty());
        // ACT
        Ganancia g = cajaService().ganancia(hoy);
        // ASSERT
        assertTrue(g.provisional());
    }

    /** RF40-CP04 (E4). Margen 0.00 %. */
    @Test
    @DisplayName("RF40-CP04 Calcular el margen.")
    void rf40_cp04_margenSinVentas() throws Exception {
        // ARRANGE
        BigDecimal ganancia = d("-30.00"), ventas = d("0.00");
        // ACT
        BigDecimal margen = Calculos.margen(ganancia, ventas);
        // ASSERT
        assertEquals(d("0.00"), margen);
    }
}
