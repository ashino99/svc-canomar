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
 * RF33 Calcular antigüedad de la deuda — pruebas TDD de la clase Calculos / CreditoServiceImpl.
 * Qué decide: calcula. Un método por caso de prueba (CP) de la ficha del Anexo B.
 */
@DisplayName("RF33 Calcular antigüedad de la deuda")
class RF33CalcularAntiguedadDeLaDeudaTest extends TddBase {

    /** RF33-CP01 (E1). S/ 732 en «8–15 días» y S/ 640 en «16–30 días». */
    @Test
    @DisplayName("RF33-CP01 Calcular la antigüedad del cliente 2.")
    void rf33_cp01_tramosPorDias() throws Exception {
        // ARRANGE
        prepararDeudaSaborNorteno();
        // ACT
        List<Antiguedad> a = creditoService().antiguedad(2);
        // ASSERT
        assertEquals(d("0.00"), a.get(0).monto());
        assertEquals(d("732.00"), a.get(1).monto());
        assertEquals(d("640.00"), a.get(2).monto());
    }

    /** RF33-CP02 (E2). «0–7 días», «8–15 días», «16–30 días» y «Más de 30 días». */
    @Test
    @DisplayName("RF33-CP02 Clasificar 7, 8, 30 y 31 días.")
    void rf33_cp02_limitesDeTramos() throws Exception {
        // ARRANGE
        long[] dias = {7, 8, 30, 31};
        // ACT
        String[] tramos = {Calculos.tramo(dias[0]), Calculos.tramo(dias[1]), Calculos.tramo(dias[2]), Calculos.tramo(dias[3])};
        // ASSERT
        assertArrayEquals(new String[]{"0–7 días", "8–15 días", "16–30 días", "Más de 30 días"}, tramos);
    }

    /** RF33-CP03 (E3). Solo «8–15 días» tiene S/ 732; «16–30 días» queda en S/ 0.00. */
    @Test
    @DisplayName("RF33-CP03 Calcular la antigüedad.")
    void rf33_cp03_ventaPagadaNoCuenta() throws Exception {
        // ARRANGE
        when(clienteDao.buscar(2)).thenReturn(Optional.of(cliente(2, "Restaurante Sabor Norteño", "1500.00", 7)));
        when(ventaDao.pendientesCliente(2)).thenReturn(List.of(
                new PendienteVenta(1L, Fechas.ahora().minusDays(20), d("640.00"), d("640.00")),
                new PendienteVenta(2L, Fechas.ahora().minusDays(10), d("732.00"), d("0.00"))));
        // ACT
        List<Antiguedad> a = creditoService().antiguedad(2);
        // ASSERT
        assertEquals(d("732.00"), a.get(1).monto());
        assertEquals(d("0.00"), a.get(2).monto());
    }

    /** RF33-CP04 (E4). Mensaje «El cliente no existe». */
    @Test
    @DisplayName("RF33-CP04 Calcular la antigüedad del cliente 99.")
    void rf33_cp04_clienteNoExiste() throws Exception {
        // ARRANGE
        when(clienteDao.buscar(99)).thenReturn(Optional.empty());
        // ACT
        NegocioException e = assertThrows(NegocioException.class, () -> creditoService().antiguedad(99));
        // ASSERT
        assertEquals("El cliente no existe", e.getMessage());
    }
}
