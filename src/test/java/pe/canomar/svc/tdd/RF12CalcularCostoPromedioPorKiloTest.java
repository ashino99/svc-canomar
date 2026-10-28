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
 * RF12 Calcular costo promedio por kilo — pruebas TDD de la clase PrecioServiceImpl.
 * Qué decide: calcula. Un método por caso de prueba (CP) de la ficha del Anexo B.
 */
@DisplayName("RF12 Calcular costo promedio por kilo")
class RF12CalcularCostoPromedioPorKiloTest extends TddBase {

    /** RF12-CP01 (E1). Costo promedio = (8×6.50 + 52×7.00) ÷ 60 = S/ 6.93. */
    @Test
    @DisplayName("RF12-CP01 Recalcular el costo del bonito.")
    void rf12_cp01_promedioPonderado() throws Exception {
        // ARRANGE
        when(loteDao.disponiblesFifo(1)).thenReturn(List.of(new LoteKg(1, d("8.00"), d("6.50")), new LoteKg(7, d("52.00"), d("7.00"))));
        when(precioDao.precioDe(eq(1), any())).thenReturn(Optional.of(d("9.00")));
        // ACT
        BigDecimal costo = precioServiceReal().recalcularCosto(1);
        // ASSERT
        assertEquals(d("6.93"), costo);
        verify(precioDao).guardarCosto(1, Fechas.hoy(), d("6.93"), d("9.00"));
    }

    /** RF12-CP02 (E2). Costo promedio = S/ 26.00. */
    @Test
    @DisplayName("RF12-CP02 Recalcular el costo de la corvina.")
    void rf12_cp02_sinSaldoAnterior() throws Exception {
        // ARRANGE
        when(loteDao.disponiblesFifo(2)).thenReturn(List.of(new LoteKg(9, d("40.00"), d("26.00"))));
        when(precioDao.precioDe(eq(2), any())).thenReturn(Optional.of(d("32.00")));
        // ACT
        BigDecimal costo = precioServiceReal().recalcularCosto(2);
        // ASSERT
        assertEquals(d("26.00"), costo);
    }

    /** RF12-CP03 (E3). Costo = 0.00 y no se guarda en precio_dia. */
    @Test
    @DisplayName("RF12-CP03 Recalcular el costo del perico.")
    void rf12_cp03_sinLotes() throws Exception {
        // ARRANGE
        when(loteDao.disponiblesFifo(7)).thenReturn(List.of());
        // ACT
        BigDecimal costo = precioServiceReal().recalcularCosto(7);
        // ASSERT
        assertEquals(d("0.00"), costo);
        verify(precioDao, never()).guardarCosto(anyInt(), any(), any(), any());
    }

    /** RF12-CP04 (E4). Se guarda costo S/ 5.50 con precio sugerido S/ 8.00 (el de ayer). */
    @Test
    @DisplayName("RF12-CP04 Recalcular el costo de la pota.")
    void rf12_cp04_precioDeAyerSugerido() throws Exception {
        // ARRANGE
        when(loteDao.disponiblesFifo(3)).thenReturn(List.of(new LoteKg(4, d("30.00"), d("5.50"))));
        when(precioDao.precioDe(eq(3), any())).thenReturn(Optional.empty());
        when(precioDao.ultimoPrecio(eq(3), any())).thenReturn(Optional.of(d("8.00")));
        // ACT
        precioServiceReal().recalcularCosto(3);
        // ASSERT
        verify(precioDao).guardarCosto(3, Fechas.hoy(), d("5.50"), d("8.00"));
    }
}
