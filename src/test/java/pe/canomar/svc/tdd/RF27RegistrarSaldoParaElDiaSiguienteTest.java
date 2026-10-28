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
 * RF27 Registrar saldo para el día siguiente — pruebas TDD de la clase SaldoServiceImpl.
 * Qué decide: cambia estado. Un método por caso de prueba (CP) de la ficha del Anexo B.
 */
@DisplayName("RF27 Registrar saldo para el día siguiente")
class RF27RegistrarSaldoParaElDiaSiguienteTest extends TddBase {

    /** RF27-CP01 (E1). Se descuentan 0.20 kg por «Diferencia al cierre del puesto» y el saldo para mañana es 5.80 kg. */
    @Test
    @DisplayName("RF27-CP01 Pesar 5.80 kg (3.3 % menos).")
    void rf27_cp01_cierreDescuentaDiferencia() throws Exception {
        // ARRANGE
        prepararCierre(5, "Jurel", "6.00");
        // ACT
        BigDecimal total = saldoService().registrarCierre(Map.of(5, d("5.80")), Map.of(), 2);
        // ASSERT
        assertEquals(d("5.80"), total);
        verify(loteDao).descontar(2, d("0.20"));
    }

    /** RF27-CP02 (E2). Se suman 0.20 kg al último lote. */
    @Test
    @DisplayName("RF27-CP02 Pesar 6.20 kg.")
    void rf27_cp02_cierreSumaDiferencia() throws Exception {
        // ARRANGE
        prepararCierre(5, "Jurel", "6.00");
        when(loteDao.ultimoLote(5)).thenReturn(Optional.of(2));
        // ACT
        saldoService().registrarCierre(Map.of(5, d("6.20")), Map.of(), 2);
        // ASSERT
        verify(loteDao).sumar(2, d("0.20"));
    }

    /** RF27-CP03 (E3). Mensaje «Jurel: diferencia de 16.67%. Indique el motivo». */
    @Test
    @DisplayName("RF27-CP03 Pesar 5.00 kg sin motivo.")
    void rf27_cp03_diferenciaPideMotivo() throws Exception {
        // ARRANGE
        prepararCierre(5, "Jurel", "6.00");
        // ACT
        NegocioException e = assertThrows(NegocioException.class,
                () -> saldoService().registrarCierre(Map.of(5, d("5.00")), Map.of(), 2));
        // ASSERT
        assertEquals("Jurel: diferencia de 16.67%. Indique el motivo", e.getMessage());
    }

    /** RF27-CP04 (E4). Mensaje «Los kilos reales no pueden ser negativos». */
    @Test
    @DisplayName("RF27-CP04 Ingresar -1 kg.")
    void rf27_cp04_kilosNegativos() throws Exception {
        // ARRANGE
        prepararCierre(5, "Jurel", "6.00");
        // ACT
        NegocioException e = assertThrows(NegocioException.class,
                () -> saldoService().registrarCierre(Map.of(5, d("-1")), Map.of(), 2));
        // ASSERT
        assertEquals("Los kilos reales no pueden ser negativos", e.getMessage());
    }
}
