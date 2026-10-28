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
 * RF30 Registrar ajuste de saldo — pruebas TDD de la clase SaldoServiceImpl.
 * Qué decide: cambia estado. Un método por caso de prueba (CP) de la ficha del Anexo B.
 */
@DisplayName("RF30 Registrar ajuste de saldo")
class RF30RegistrarAjusteDeSaldoTest extends TddBase {

    /** RF30-CP01 (E1). Se descuentan 0.50 kg y se registra el ajuste con valor S/ 2.50. */
    @Test
    @DisplayName("RF30-CP01 Ajuste MERMA de -0.5 kg «Pérdida de agua».")
    void rf30_cp01_ajusteMerma() throws Exception {
        // ARRANGE
        when(productoDao.buscar(5)).thenReturn(Optional.of(producto(5, "Jurel")));
        when(loteDao.kilosDisponibles(5)).thenReturn(d("6.00"));
        when(loteDao.disponiblesFifo(5)).thenReturn(List.of(new LoteKg(2, d("6.00"), d("5.00"))));
        // ACT
        BigDecimal valor = saldoService().registrarAjuste(5, d("-0.5"), "MERMA", "Pérdida de agua", 1);
        // ASSERT
        assertEquals(d("2.50"), valor);
        verify(loteDao).insertarAjuste(eq(2), eq(1), any(), eq("MERMA"), eq(d("-0.50")), eq(d("2.50")), eq("Pérdida de agua"));
    }

    /** RF30-CP02 (E2). Se suma 1.00 kg al lote 4 y el valor es S/ -5.50 (no es pérdida). */
    @Test
    @DisplayName("RF30-CP02 Ajuste CORRECCION de +1 kg «Error de conteo».")
    void rf30_cp02_ajustePositivo() throws Exception {
        // ARRANGE
        when(productoDao.buscar(3)).thenReturn(Optional.of(producto(3, "Pota")));
        when(precioDao.ultimoCosto(eq(3), any())).thenReturn(Optional.of(d("5.50")));
        when(loteDao.ultimoLote(3)).thenReturn(Optional.of(4));
        // ACT
        BigDecimal valor = saldoService().registrarAjuste(3, d("1"), "CORRECCION", "Error de conteo", 1);
        // ASSERT
        assertEquals(d("-5.50"), valor);
        verify(loteDao).sumar(4, d("1.00"));
    }

    /** RF30-CP03 (E3). Mensaje «Indique el motivo del ajuste». */
    @Test
    @DisplayName("RF30-CP03 Ajuste MERMA sin motivo.")
    void rf30_cp03_faltaMotivo() throws Exception {
        // ARRANGE
        when(productoDao.buscar(5)).thenReturn(Optional.of(producto(5, "Jurel")));
        // ACT
        NegocioException e = assertThrows(NegocioException.class,
                () -> saldoService().registrarAjuste(5, d("-0.5"), "MERMA", "", 1));
        // ASSERT
        assertEquals("Indique el motivo del ajuste", e.getMessage());
    }

    /** RF30-CP04 (E4). Mensaje «Solo hay 6.00 kg de jurel: el saldo no puede quedar negativo». */
    @Test
    @DisplayName("RF30-CP04 Ajuste DESECHO de -7 kg «Malogrado».")
    void rf30_cp04_saldoNegativo() throws Exception {
        // ARRANGE
        when(productoDao.buscar(5)).thenReturn(Optional.of(producto(5, "Jurel")));
        when(loteDao.kilosDisponibles(5)).thenReturn(d("6.00"));
        // ACT
        NegocioException e = assertThrows(NegocioException.class,
                () -> saldoService().registrarAjuste(5, d("-7"), "DESECHO", "Malogrado", 1));
        // ASSERT
        assertEquals("Solo hay 6.00 kg de jurel: el saldo no puede quedar negativo", e.getMessage());
    }
}
