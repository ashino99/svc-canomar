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
 * RF04 Cambiar contraseña — pruebas TDD de la clase UsuarioServiceImpl.
 * Qué decide: valida. Un método por caso de prueba (CP) de la ficha del Anexo B.
 */
@DisplayName("RF04 Cambiar contraseña")
class RF04CambiarContrasenaTest extends TddBase {

    /** RF04-CP01 (E1). Se guarda la nueva clave cifrada y se registra CAMBIAR_CLAVE. */
    @Test
    @DisplayName("RF04-CP01 Cambiar a «Corvina2027» (repetida igual).")
    void rf04_cp01_cambiaClave() throws Exception {
        // ARRANGE
        when(usuarioDao.buscarPorId(2)).thenReturn(Optional.of(usuarioConClave(2, "vendedor", "Canomar2026")));
        // ACT
        usuarioService().cambiarClave(2, "Canomar2026", "Corvina2027", "Corvina2027");
        // ASSERT
        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(usuarioDao).actualizarClave(eq(2), hash.capture());
        assertTrue(encoder.matches("Corvina2027", hash.getValue()));
        verify(auditoria).registrar(2, "CAMBIAR_CLAVE", "vendedor");
    }

    /** RF04-CP02 (E2). Mensaje «Las contraseñas no coinciden». */
    @Test
    @DisplayName("RF04-CP02 Nueva «Corvina2027», repetir «Corvina2028».")
    void rf04_cp02_noCoinciden() throws Exception {
        // ARRANGE (la regla se valida antes de consultar el DAO)
        // ACT
        NegocioException e = assertThrows(NegocioException.class,
                () -> usuarioService().cambiarClave(2, "Canomar2026", "Corvina2027", "Corvina2028"));
        // ASSERT
        assertEquals("Las contraseñas no coinciden", e.getMessage());
        verify(usuarioDao, never()).actualizarClave(anyInt(), anyString());
    }

    /** RF04-CP03 (E3). Mensaje «La contraseña actual es incorrecta». */
    @Test
    @DisplayName("RF04-CP03 Clave actual «Canomar2025».")
    void rf04_cp03_actualIncorrecta() throws Exception {
        // ARRANGE
        when(usuarioDao.buscarPorId(2)).thenReturn(Optional.of(usuarioConClave(2, "vendedor", "Canomar2026")));
        // ACT
        NegocioException e = assertThrows(NegocioException.class,
                () -> usuarioService().cambiarClave(2, "Canomar2025", "Corvina2027", "Corvina2027"));
        // ASSERT
        assertEquals("La contraseña actual es incorrecta", e.getMessage());
    }

    /** RF04-CP04 (E4). Mensaje «La contraseña debe tener al menos 8 caracteres». */
    @Test
    @DisplayName("RF04-CP04 Nueva clave «pez» repetida igual.")
    void rf04_cp04_nuevaCorta() throws Exception {
        // ARRANGE
        // ACT
        NegocioException e = assertThrows(NegocioException.class,
                () -> usuarioService().cambiarClave(2, "Canomar2026", "pez", "pez"));
        // ASSERT
        assertEquals("La contraseña debe tener al menos 8 caracteres", e.getMessage());
    }
}
