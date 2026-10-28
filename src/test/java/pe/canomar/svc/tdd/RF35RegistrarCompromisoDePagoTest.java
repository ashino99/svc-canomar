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
 * RF35 Registrar compromiso de pago — pruebas TDD de la clase CreditoServiceImpl.
 * Qué decide: cambia estado. Un método por caso de prueba (CP) de la ficha del Anexo B.
 */
@DisplayName("RF35 Registrar compromiso de pago")
class RF35RegistrarCompromisoDePagoTest extends TddBase {

    /** RF35-CP01 (E1). Se guarda el compromiso y la bitácora COMPROMISO_PAGO. */
    @Test
    @DisplayName("RF35-CP01 Registrar compromiso de S/ 500 para dentro de 3 días.")
    void rf35_cp01_registraCompromiso() throws Exception {
        // ARRANGE
        prepararDeudaSaborNorteno();
        LocalDate fecha = Fechas.hoy().plusDays(3);
        // ACT
        creditoService().registrarCompromiso(2, fecha, d("500"), 1);
        // ASSERT
        verify(compromisoDao).insertar(2, fecha, d("500.00"));
        verify(auditoria).registrar(eq(1), eq("COMPROMISO_PAGO"), startsWith("Restaurante Sabor Norteño · S/ 500.00"));
    }

    /** RF35-CP02 (E2). Antes de listar se marcan como incumplidos los vencidos al día de hoy. */
    @Test
    @DisplayName("RF35-CP02 Abrir la lista de compromisos.")
    void rf35_cp02_marcaIncumplidos() throws Exception {
        // ARRANGE
        when(compromisoDao.listarVigentes()).thenReturn(List.of());
        // ACT
        creditoService().compromisos();
        // ASSERT
        InOrder orden = inOrder(compromisoDao);
        orden.verify(compromisoDao).marcarIncumplidos(Fechas.hoy());
        orden.verify(compromisoDao).listarVigentes();
    }

    /** RF35-CP03 (E3). Mensaje «La fecha debe ser hoy o posterior». */
    @Test
    @DisplayName("RF35-CP03 Registrar compromiso con fecha de ayer.")
    void rf35_cp03_fechaPasada() throws Exception {
        // ARRANGE
        LocalDate ayer = Fechas.hoy().minusDays(1);
        // ACT
        NegocioException e = assertThrows(NegocioException.class, () -> creditoService().registrarCompromiso(2, ayer, d("500"), 1));
        // ASSERT
        assertEquals("La fecha debe ser hoy o posterior", e.getMessage());
    }

    /** RF35-CP04 (E4). Mensaje «El cliente no tiene deudas». */
    @Test
    @DisplayName("RF35-CP04 Registrar compromiso de S/ 100.")
    void rf35_cp04_sinDeudas() throws Exception {
        // ARRANGE
        when(clienteDao.buscar(3)).thenReturn(Optional.of(cliente(3, "Pollería Don Lucho", "800.00", 7)));
        when(ventaDao.pendientesCliente(3)).thenReturn(List.of());
        // ACT
        NegocioException e = assertThrows(NegocioException.class,
                () -> creditoService().registrarCompromiso(3, Fechas.hoy(), d("100"), 1));
        // ASSERT
        assertEquals("El cliente no tiene deudas", e.getMessage());
    }
}
