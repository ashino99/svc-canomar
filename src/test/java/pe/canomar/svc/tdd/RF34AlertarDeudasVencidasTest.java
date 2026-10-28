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
 * RF34 Alertar deudas vencidas — pruebas TDD de la clase CreditoServiceImpl.
 * Qué decide: valida. Un método por caso de prueba (CP) de la ficha del Anexo B.
 */
@DisplayName("RF34 Alertar deudas vencidas")
class RF34AlertarDeudasVencidasTest extends TddBase {

    /** RF34-CP01 (E1). Solo aparece el cliente 2. */
    @Test
    @DisplayName("RF34-CP01 Consultar vencidos.")
    void rf34_cp01_soloVencidos() throws Exception {
        // ARRANGE
        when(ventaDao.saldosClientes(any())).thenReturn(List.of(saldoCliente(1, "228.00", "1500.00", "0.00"), saldoCliente(2, "1372.00", "1500.00", "1372.00")));
        // ACT
        List<SaldoCliente> v = creditoService().vencidos();
        // ASSERT
        assertEquals(1, v.size());
        assertEquals(2, v.get(0).idCliente());
    }

    /** RF34-CP02 (E2). Lista vacía. */
    @Test
    @DisplayName("RF34-CP02 Consultar vencidos.")
    void rf34_cp02_ningunoVencido() throws Exception {
        // ARRANGE
        when(ventaDao.saldosClientes(any())).thenReturn(List.of(saldoCliente(1, "228.00", "1500.00", "0.00")));
        // ACT
        List<SaldoCliente> v = creditoService().vencidos();
        // ASSERT
        assertTrue(v.isEmpty());
    }

    /** RF34-CP03 (E3). Aparece el cliente 3 por estar sobre su límite. */
    @Test
    @DisplayName("RF34-CP03 Consultar vencidos.")
    void rf34_cp03_sobreLimite() throws Exception {
        // ARRANGE
        when(ventaDao.saldosClientes(any())).thenReturn(List.of(saldoCliente(3, "900.00", "800.00", "0.00")));
        // ACT
        List<SaldoCliente> v = creditoService().vencidos();
        // ASSERT
        assertEquals(1, v.size());
        assertTrue(v.get(0).sobreLimite());
    }
}
