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
 * RF31 Registrar abono de cliente — pruebas TDD de la clase CreditoServiceImpl.
 * Qué decide: cambia estado. Un método por caso de prueba (CP) de la ficha del Anexo B.
 */
@DisplayName("RF31 Registrar abono de cliente")
class RF31RegistrarAbonoDeClienteTest extends TddBase {

    /** RF31-CP01 (E1). Se aplican S/ 640 a la venta 1 y S/ 160 a la venta 2; nuevo saldo S/ 572.00. */
    @Test
    @DisplayName("RF31-CP01 Abonar S/ 800 en efectivo.")
    void rf31_cp01_aplicaALaMasAntigua() throws Exception {
        // ARRANGE
        prepararDeudaSaborNorteno();
        when(abonoDao.insertar(anyInt(), anyInt(), any(), any(), anyString())).thenReturn(5);
        // ACT
        BigDecimal saldo = creditoService().registrarAbono(2, d("800"), "EFECTIVO", 1);
        // ASSERT
        assertEquals(d("572.00"), saldo);
        verify(abonoDao).aplicar(5, 1L, d("640.00"));
        verify(abonoDao).aplicar(5, 2L, d("160.00"));
    }

    /** RF31-CP02 (E2). Saldo S/ 0.00 y se marcan sus compromisos como cumplidos. */
    @Test
    @DisplayName("RF31-CP02 Abonar S/ 1,372 por Yape.")
    void rf31_cp02_abonoTotalCumpleCompromisos() throws Exception {
        // ARRANGE
        prepararDeudaSaborNorteno();
        // ACT
        BigDecimal saldo = creditoService().registrarAbono(2, d("1372"), "YAPE", 1);
        // ASSERT
        assertEquals(d("0.00"), saldo);
        verify(compromisoDao).marcarCumplidos(2);
    }

    /** RF31-CP03 (E3). Mensaje «El abono supera el saldo (S/ 1372.00)». */
    @Test
    @DisplayName("RF31-CP03 Abonar S/ 2,000.")
    void rf31_cp03_superaSaldo() throws Exception {
        // ARRANGE
        prepararDeudaSaborNorteno();
        // ACT
        NegocioException e = assertThrows(NegocioException.class, () -> creditoService().registrarAbono(2, d("2000"), "EFECTIVO", 1));
        // ASSERT
        assertEquals("El abono supera el saldo (S/ 1372.00)", e.getMessage());
        verify(abonoDao, never()).insertar(anyInt(), anyInt(), any(), any(), anyString());
    }

    /** RF31-CP04 (E4). Mensaje «Elija el medio de pago». */
    @Test
    @DisplayName("RF31-CP04 Abonar S/ 100 sin medio de pago.")
    void rf31_cp04_faltaMedio() throws Exception {
        // ARRANGE
        // ACT
        NegocioException e = assertThrows(NegocioException.class, () -> creditoService().registrarAbono(2, d("100"), null, 1));
        // ASSERT
        assertEquals("Elija el medio de pago", e.getMessage());
    }
}
