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
 * RF28 Consultar saldo por antigüedad — pruebas TDD de la clase Calculos / SaldoServiceImpl.
 * Qué decide: calcula. Un método por caso de prueba (CP) de la ficha del Anexo B.
 */
@DisplayName("RF28 Consultar saldo por antigüedad")
class RF28ConsultarSaldoPorAntiguedadTest extends TddBase {

    /** RF28-CP01 (E1). Color «verde». */
    @Test
    @DisplayName("RF28-CP01 Clasificar un lote de hoy (0 días).")
    void rf28_cp01_loteDeHoyVerde() throws Exception {
        // ARRANGE
        int diasAlerta = 3;
        // ACT
        String color = Calculos.colorSaldo(0, diasAlerta);
        // ASSERT
        assertEquals("verde", color);
    }

    /** RF28-CP02 (E2). Color «naranja». */
    @Test
    @DisplayName("RF28-CP02 Clasificar un lote de 2 días.")
    void rf28_cp02_unDiaAntesNaranja() throws Exception {
        // ARRANGE
        int diasAlerta = 3;
        // ACT
        String color = Calculos.colorSaldo(2, diasAlerta);
        // ASSERT
        assertEquals("naranja", color);
    }

    /** RF28-CP03 (E3). Color «rojo». */
    @Test
    @DisplayName("RF28-CP03 Clasificar un lote de 3 días.")
    void rf28_cp03_enAlertaRojo() throws Exception {
        // ARRANGE
        int diasAlerta = 3;
        // ACT
        String color = Calculos.colorSaldo(3, diasAlerta);
        // ASSERT
        assertEquals("rojo", color);
    }

    /** RF28-CP04 (E4). La línea trae 3 días y color «rojo». */
    @Test
    @DisplayName("RF28-CP04 Consultar el saldo.")
    void rf28_cp04_saldoConDias() throws Exception {
        // ARRANGE
        when(configDao.obtener()).thenReturn(config());
        when(loteDao.lotesConKilos()).thenReturn(List.of(lote(1, 1, "Bonito", 3, "8.00", "6.50")));
        // ACT
        List<SaldoLinea> saldo = saldoService().saldo();
        // ASSERT
        assertEquals(3, saldo.get(0).dias());
        assertEquals("rojo", saldo.get(0).color());
    }
}
