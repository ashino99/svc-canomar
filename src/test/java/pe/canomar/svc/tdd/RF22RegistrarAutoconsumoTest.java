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
 * RF22 Registrar autoconsumo — pruebas TDD de la clase SaldoServiceImpl.
 * Qué decide: calcula. Un método por caso de prueba (CP) de la ficha del Anexo B.
 */
@DisplayName("RF22 Registrar autoconsumo")
class RF22RegistrarAutoconsumoTest extends TddBase {

    /** RF22-CP01 (E1). Se descuentan 1.50 kg del lote 2 y el valor es S/ 7.50. */
    @Test
    @DisplayName("RF22-CP01 Registrar 1.5 kg de autoconsumo.")
    void rf22_cp01_autoconsumoValorizado() throws Exception {
        // ARRANGE
        when(productoDao.buscar(5)).thenReturn(Optional.of(producto(5, "Jurel")));
        when(loteDao.kilosDisponibles(5)).thenReturn(d("6.00"));
        when(loteDao.disponiblesFifo(5)).thenReturn(List.of(new LoteKg(2, d("6.00"), d("5.00"))));
        // ACT
        BigDecimal valor = saldoService().registrarAutoconsumo(5, d("1.5"), 2);
        // ASSERT
        assertEquals(d("7.50"), valor);
        verify(loteDao).descontar(2, d("1.50"));
    }

    /** RF22-CP02 (E2). Se toman 2 kg del lote 1 y 1 kg del lote 7; valor S/ 20.00. */
    @Test
    @DisplayName("RF22-CP02 Registrar 3 kg de autoconsumo.")
    void rf22_cp02_fifoPrimeroElAntiguo() throws Exception {
        // ARRANGE
        when(productoDao.buscar(1)).thenReturn(Optional.of(producto(1, "Bonito")));
        when(loteDao.kilosDisponibles(1)).thenReturn(d("12.00"));
        when(loteDao.disponiblesFifo(1)).thenReturn(List.of(new LoteKg(1, d("2.00"), d("6.50")), new LoteKg(7, d("10.00"), d("7.00"))));
        // ACT
        BigDecimal valor = saldoService().registrarAutoconsumo(1, d("3"), 2);
        // ASSERT
        assertEquals(d("20.00"), valor);
        InOrder orden = inOrder(loteDao);
        orden.verify(loteDao).descontar(1, d("2.00"));
        orden.verify(loteDao).descontar(7, d("1.00"));
    }

    /** RF22-CP03 (E3). Mensaje «Solo hay 6.00 kg de jurel: el saldo no puede quedar negativo». */
    @Test
    @DisplayName("RF22-CP03 Registrar 8 kg de autoconsumo.")
    void rf22_cp03_superaSaldo() throws Exception {
        // ARRANGE
        when(productoDao.buscar(5)).thenReturn(Optional.of(producto(5, "Jurel")));
        when(loteDao.kilosDisponibles(5)).thenReturn(d("6.00"));
        // ACT
        NegocioException e = assertThrows(NegocioException.class, () -> saldoService().registrarAutoconsumo(5, d("8"), 2));
        // ASSERT
        assertEquals("Solo hay 6.00 kg de jurel: el saldo no puede quedar negativo", e.getMessage());
        verify(loteDao, never()).descontar(anyInt(), any());
    }

    /** RF22-CP04 (E4). Mensaje «Ingrese los kilos». */
    @Test
    @DisplayName("RF22-CP04 Registrar 0 kg.")
    void rf22_cp04_kilosCero() throws Exception {
        // ARRANGE
        when(productoDao.buscar(5)).thenReturn(Optional.of(producto(5, "Jurel")));
        // ACT
        NegocioException e = assertThrows(NegocioException.class, () -> saldoService().registrarAutoconsumo(5, d("0"), 2));
        // ASSERT
        assertEquals("Ingrese los kilos", e.getMessage());
    }
}
