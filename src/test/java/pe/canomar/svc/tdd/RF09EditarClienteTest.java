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
 * RF09 Editar cliente — pruebas TDD de la clase ClienteServiceImpl.
 * Qué decide: valida. Un método por caso de prueba (CP) de la ficha del Anexo B.
 */
@DisplayName("RF09 Editar cliente")
class RF09EditarClienteTest extends TddBase {

    /** RF09-CP01 (E1). Bitácora CAMBIO_LIMITE «S/ 1500.00 → S/ 2000.00»; sin advertencia. */
    @Test
    @DisplayName("RF09-CP01 Subir el límite a S/ 2,000.")
    void rf09_cp01_cambiaLimite() throws Exception {
        // ARRANGE
        when(clienteDao.buscar(1)).thenReturn(Optional.of(cliente(1, "Cevichería El Muelle", "1500.00", 15)));
        when(credito.saldo(1)).thenReturn(saldoCliente(1, "228.00", "1500.00", "0.00"));
        // ACT
        String aviso = clienteService().editar(1, "Cevichería El Muelle", "20601234561", "987654321", d("2000"), 15, "ACTIVO", 1);
        // ASSERT
        assertNull(aviso);
        verify(auditoria).registrar(1, "CAMBIO_LIMITE", "Cevichería El Muelle: S/ 1500.00 → S/ 2000.00");
    }

    /** RF09-CP02 (E2). Aviso «El saldo actual (S/ 1372.00) supera el nuevo límite: no podrá comprar al crédito hasta abonar». */
    @Test
    @DisplayName("RF09-CP02 Bajar su límite a S/ 1,000.")
    void rf09_cp02_limiteMenorQueSaldo() throws Exception {
        // ARRANGE
        when(clienteDao.buscar(2)).thenReturn(Optional.of(cliente(2, "Restaurante Sabor Norteño", "1500.00", 7)));
        when(credito.saldo(2)).thenReturn(saldoCliente(2, "1372.00", "1000.00", "1372.00"));
        // ACT
        String aviso = clienteService().editar(2, "Restaurante Sabor Norteño", "20601234571", null, d("1000"), 7, "ACTIVO", 1);
        // ASSERT
        assertEquals("El saldo actual (S/ 1372.00) supera el nuevo límite: no podrá comprar al crédito hasta abonar", aviso);
    }

    /** RF09-CP03 (E3). Mensaje «El plazo debe estar entre 0 y 90 días». */
    @Test
    @DisplayName("RF09-CP03 Cambiar el plazo a 120 días.")
    void rf09_cp03_plazoFueraDeRango() throws Exception {
        // ARRANGE
        when(clienteDao.buscar(1)).thenReturn(Optional.of(cliente(1, "Cevichería El Muelle", "1500.00", 15)));
        // ACT
        NegocioException e = assertThrows(NegocioException.class,
                () -> clienteService().editar(1, "Cevichería El Muelle", null, null, d("1500"), 120, "ACTIVO", 1));
        // ASSERT
        assertEquals("El plazo debe estar entre 0 y 90 días", e.getMessage());
    }

    /** RF09-CP04 (E4). Mensaje «El límite de crédito no puede ser negativo». */
    @Test
    @DisplayName("RF09-CP04 Cambiar el límite a S/ -50.")
    void rf09_cp04_limiteNegativo() throws Exception {
        // ARRANGE
        when(clienteDao.buscar(1)).thenReturn(Optional.of(cliente(1, "Cevichería El Muelle", "1500.00", 15)));
        // ACT
        NegocioException e = assertThrows(NegocioException.class,
                () -> clienteService().editar(1, "Cevichería El Muelle", null, null, d("-50"), 15, "ACTIVO", 1));
        // ASSERT
        assertEquals("El límite de crédito no puede ser negativo", e.getMessage());
    }
}
