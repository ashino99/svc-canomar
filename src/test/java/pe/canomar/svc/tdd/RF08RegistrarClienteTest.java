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
 * RF08 Registrar cliente — pruebas TDD de la clase ClienteServiceImpl.
 * Qué decide: valida. Un método por caso de prueba (CP) de la ficha del Anexo B.
 */
@DisplayName("RF08 Registrar cliente")
class RF08RegistrarClienteTest extends TddBase {

    /** RF08-CP01 (E1). Se inserta el cliente ACTIVO y la bitácora dice «límite S/ 1000.00». */
    @Test
    @DisplayName("RF08-CP01 Registrar «Cevichería La Ola», RUC 20605555551, límite S/ 1,000, plazo 15.")
    void rf08_cp01_registraCliente() throws Exception {
        // ARRANGE
        when(clienteDao.existeRuc("20605555551", null)).thenReturn(false);
        when(clienteDao.insertar(any())).thenReturn(4);
        // ACT
        int id = clienteService().registrar("Cevichería La Ola", "20605555551", "988777666", d("1000"), 15, 1);
        // ASSERT
        assertEquals(4, id);
        verify(clienteDao).insertar(new Cliente(null, "Cevichería La Ola", "20605555551", "988777666", d("1000.00"), 15, "ACTIVO"));
        verify(auditoria).registrar(1, "REGISTRAR_CLIENTE", "Cevichería La Ola · límite S/ 1000.00");
    }

    /** RF08-CP02 (E2). Se inserta con RUC vacío (null). */
    @Test
    @DisplayName("RF08-CP02 Registrar «Juguería Rosita» sin RUC, límite S/ 300.")
    void rf08_cp02_sinRuc() throws Exception {
        // ARRANGE
        // ACT
        clienteService().registrar("Juguería Rosita", "", null, d("300"), 7, 1);
        // ASSERT
        ArgumentCaptor<Cliente> c = ArgumentCaptor.forClass(Cliente.class);
        verify(clienteDao).insertar(c.capture());
        assertNull(c.getValue().ruc());
    }

    /** RF08-CP03 (E3). Mensaje «El RUC debe tener 11 dígitos». */
    @Test
    @DisplayName("RF08-CP03 Registrar con RUC «2060123».")
    void rf08_cp03_rucInvalido() throws Exception {
        // ARRANGE
        // ACT
        NegocioException e = assertThrows(NegocioException.class,
                () -> clienteService().registrar("Cevichería La Ola", "2060123", null, d("1000"), 15, 1));
        // ASSERT
        assertEquals("El RUC debe tener 11 dígitos", e.getMessage());
    }

    /** RF08-CP04 (E4). Mensaje «El cliente ya está registrado con ese RUC». */
    @Test
    @DisplayName("RF08-CP04 Registrar otro cliente con ese RUC.")
    void rf08_cp04_rucDuplicado() throws Exception {
        // ARRANGE
        when(clienteDao.existeRuc("20601234561", null)).thenReturn(true);
        // ACT
        NegocioException e = assertThrows(NegocioException.class,
                () -> clienteService().registrar("El Muelle 2", "20601234561", null, d("500"), 7, 1));
        // ASSERT
        assertEquals("El cliente ya está registrado con ese RUC", e.getMessage());
    }
}
