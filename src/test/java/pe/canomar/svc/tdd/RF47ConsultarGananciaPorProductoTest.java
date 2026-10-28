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
 * RF47 Consultar ganancia por producto — pruebas TDD de la clase ReporteServiceImpl.
 * Qué decide: calcula. Un método por caso de prueba (CP) de la ficha del Anexo B.
 */
@DisplayName("RF47 Consultar ganancia por producto")
class RF47ConsultarGananciaPorProductoTest extends TddBase {

    /** RF47-CP01 (E1). Ganancia S/ 120.00 y margen 18.8 %. */
    @Test
    @DisplayName("RF47-CP01 Calcular ganancia y margen.")
    void rf47_cp01_gananciaYMargen() throws Exception {
        // ARRANGE
        GananciaProducto corvina = new GananciaProducto("Corvina", d("20.00"), d("640.00"), d("520.00"));
        // ACT
        BigDecimal ganancia = corvina.ganancia();
        BigDecimal margen = corvina.margen();
        // ASSERT
        assertEquals(d("120.00"), ganancia);
        assertEquals(new BigDecimal("18.8"), margen);
    }

    /** RF47-CP02 (E2). Ganancia S/ -2.00. */
    @Test
    @DisplayName("RF47-CP02 Calcular la ganancia.")
    void rf47_cp02_perdidaPorRemate() throws Exception {
        // ARRANGE
        GananciaProducto bonito = new GananciaProducto("Bonito", d("4.00"), d("24.00"), d("26.00"));
        // ACT
        BigDecimal ganancia = bonito.ganancia();
        // ASSERT
        assertEquals(d("-2.00"), ganancia);
        assertTrue(ganancia.signum() < 0);
    }

    /** RF47-CP03 (E3). Mensaje «Revise el rango de fechas». */
    @Test
    @DisplayName("RF47-CP03 Consultar del 31/10 al 01/10.")
    void rf47_cp03_rangoInvertido() throws Exception {
        // ARRANGE
        LocalDate desde = LocalDate.of(2026, 10, 31), hasta = LocalDate.of(2026, 10, 1);
        // ACT
        NegocioException e = assertThrows(NegocioException.class, () -> reporteService().gananciaPorProducto(desde, hasta));
        // ASSERT
        assertEquals("Revise el rango de fechas", e.getMessage());
    }

    /** RF47-CP04 (E4). Margen 0.00 %. */
    @Test
    @DisplayName("RF47-CP04 Calcular su margen.")
    void rf47_cp04_sinVentasMargenCero() throws Exception {
        // ARRANGE
        GananciaProducto perico = new GananciaProducto("Perico", d("0.00"), d("0.00"), d("0.00"));
        // ACT
        BigDecimal margen = perico.margen();
        // ASSERT
        assertEquals(d("0.00"), margen);
    }
}
