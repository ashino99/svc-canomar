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
 * RF05 Desactivar usuario — pruebas TDD de la clase UsuarioServiceImpl.
 * Qué decide: cambia estado. Un método por caso de prueba (CP) de la ficha del Anexo B.
 */
@DisplayName("RF05 Desactivar usuario")
class RF05DesactivarUsuarioTest extends TddBase {

    /** RF05-CP01 (E1). Estado del usuario 2 = INACTIVO; bitácora DESACTIVAR_USUARIO. */
    @Test
    @DisplayName("RF05-CP01 El administrador 1 desactiva al usuario 2.")
    void rf05_cp01_desactivaUsuario() throws Exception {
        // ARRANGE
        when(usuarioDao.buscarPorId(2)).thenReturn(Optional.of(usuario(2, "vendedor", "VENDEDOR", "ACTIVO", 0)));
        // ACT
        usuarioService().desactivar(2, 1);
        // ASSERT
        verify(usuarioDao).cambiarEstado(2, "INACTIVO");
        verify(auditoria).registrar(1, "DESACTIVAR_USUARIO", "vendedor");
    }

    /** RF05-CP02 (E2). Mensaje «No puede desactivar su propia cuenta». */
    @Test
    @DisplayName("RF05-CP02 Desactivar al usuario 1 (él mismo).")
    void rf05_cp02_propiaCuenta() throws Exception {
        // ARRANGE
        when(usuarioDao.buscarPorId(1)).thenReturn(Optional.of(usuario(1, "cuentas", "ADMINISTRADOR", "ACTIVO", 0)));
        // ACT
        NegocioException e = assertThrows(NegocioException.class, () -> usuarioService().desactivar(1, 1));
        // ASSERT
        assertEquals("No puede desactivar su propia cuenta", e.getMessage());
        verify(usuarioDao, never()).cambiarEstado(anyInt(), anyString());
    }

    /** RF05-CP03 (E3). Mensaje «No puede desactivar al único administrador». */
    @Test
    @DisplayName("RF05-CP03 Otro usuario intenta desactivar al administrador 4.")
    void rf05_cp03_unicoAdministrador() throws Exception {
        // ARRANGE
        when(usuarioDao.buscarPorId(4)).thenReturn(Optional.of(usuario(4, "dueña", "ADMINISTRADOR", "ACTIVO", 0)));
        when(usuarioDao.contarAdministradoresActivos()).thenReturn(1);
        // ACT
        NegocioException e = assertThrows(NegocioException.class, () -> usuarioService().desactivar(4, 1));
        // ASSERT
        assertEquals("No puede desactivar al único administrador", e.getMessage());
    }

    /** RF05-CP04 (E4). Estado del usuario 2 = ACTIVO; bitácora ACTIVAR_USUARIO. */
    @Test
    @DisplayName("RF05-CP04 Activar al usuario 2.")
    void rf05_cp04_activaUsuario() throws Exception {
        // ARRANGE
        when(usuarioDao.buscarPorId(2)).thenReturn(Optional.of(usuario(2, "vendedor", "VENDEDOR", "INACTIVO", 0)));
        // ACT
        usuarioService().activar(2, 1);
        // ASSERT
        verify(usuarioDao).cambiarEstado(2, "ACTIVO");
        verify(auditoria).registrar(1, "ACTIVAR_USUARIO", "vendedor");
    }
}
