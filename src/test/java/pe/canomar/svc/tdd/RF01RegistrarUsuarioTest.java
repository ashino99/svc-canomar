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
 * RF01 Registrar usuario — pruebas TDD de la clase UsuarioServiceImpl.
 * Qué decide: valida. Un método por caso de prueba (CP) de la ficha del Anexo B.
 */
@DisplayName("RF01 Registrar usuario")
class RF01RegistrarUsuarioTest extends TddBase {

    /** RF01-CP01 (E1). Se guarda el usuario con la clave cifrada (empieza con $2a$) y se registra REGISTRAR_USUARIO en la bitácora. */
    @Test
    @DisplayName("RF01-CP01 Registrar «Caja del puesto», usuario «caja2», rol VENDEDOR, clave «Pescado2026».")
    void rf01_cp01_registraUsuarioConClaveCifrada() throws Exception {
        // ARRANGE
        when(usuarioDao.existeUsuario("caja2")).thenReturn(false);
        when(usuarioDao.insertar(anyString(), anyString(), anyString(), anyString())).thenReturn(4);
        // ACT
        int id = usuarioService().registrar("Caja del puesto", "caja2", "VENDEDOR", "Pescado2026", 1);
        // ASSERT
        assertEquals(4, id);
        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(usuarioDao).insertar(eq("Caja del puesto"), eq("caja2"), hash.capture(), eq("VENDEDOR"));
        assertTrue(hash.getValue().startsWith("$2a$"), "la clave debe guardarse cifrada con BCrypt");
        verify(auditoria).registrar(1, "REGISTRAR_USUARIO", "caja2 · VENDEDOR");
    }

    /** RF01-CP02 (E2). Mensaje «El usuario ya existe, ingrese otro»; no se inserta nada. */
    @Test
    @DisplayName("RF01-CP02 Registrar otra cuenta con el usuario «vendedor».")
    void rf01_cp02_usuarioDuplicado() throws Exception {
        // ARRANGE
        when(usuarioDao.existeUsuario("vendedor")).thenReturn(true);
        // ACT
        NegocioException e = assertThrows(NegocioException.class,
                () -> usuarioService().registrar("Otro vendedor", "vendedor", "VENDEDOR", "Pescado2026", 1));
        // ASSERT
        assertEquals("El usuario ya existe, ingrese otro", e.getMessage());
        verify(usuarioDao, never()).insertar(anyString(), anyString(), anyString(), anyString());
    }

    /** RF01-CP03 (E3). Mensaje «La contraseña debe tener al menos 8 caracteres». */
    @Test
    @DisplayName("RF01-CP03 Registrar el usuario con la clave «abc123» (6 caracteres).")
    void rf01_cp03_claveCorta() throws Exception {
        // ARRANGE
        when(usuarioDao.existeUsuario("caja2")).thenReturn(false);
        // ACT
        NegocioException e = assertThrows(NegocioException.class,
                () -> usuarioService().registrar("Caja del puesto", "caja2", "VENDEDOR", "abc123", 1));
        // ASSERT
        assertEquals("La contraseña debe tener al menos 8 caracteres", e.getMessage());
    }

    /** RF01-CP04 (E4). Mensaje «Elija un rol válido». */
    @Test
    @DisplayName("RF01-CP04 Registrar el usuario con el rol «GERENTE».")
    void rf01_cp04_rolInvalido() throws Exception {
        // ARRANGE (no se necesita preparar el DAO: la regla falla antes)
        // ACT
        NegocioException e = assertThrows(NegocioException.class,
                () -> usuarioService().registrar("Caja del puesto", "caja2", "GERENTE", "Pescado2026", 1));
        // ASSERT
        assertEquals("Elija un rol válido", e.getMessage());
        verify(auditoria, never()).registrar(any(), any(), any());
    }
}
