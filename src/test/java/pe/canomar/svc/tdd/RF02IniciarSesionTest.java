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
 * RF02 Iniciar sesión — pruebas TDD de la clase ManejadorAcceso.
 * Qué decide: cambia estado. Un método por caso de prueba (CP) de la ficha del Anexo B.
 */
@DisplayName("RF02 Iniciar sesión")
class RF02IniciarSesionTest extends TddBase {

    /** RF02-CP01 (E1). Intentos en 0, registro INICIAR_SESION en la bitácora y redirección a /inicio. */
    @Test
    @DisplayName("RF02-CP01 Ingresar con usuario y contraseña correctos.")
    void rf02_cp01_ingresoCorrectoReiniciaIntentos() throws Exception {
        // ARRANGE
        Usuario u = usuario(2, "vendedor", "VENDEDOR", "ACTIVO", 2);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(new UsuarioSesion(u));
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        when(req.getContextPath()).thenReturn("");
        // ACT
        manejadorAcceso().onAuthenticationSuccess(req, resp, auth);
        // ASSERT
        verify(usuarioDao).actualizarIntentos(2, 0, null);
        verify(auditoriaDao).registrar(2, "INICIAR_SESION", "Ingreso de vendedor");
        verify(resp).sendRedirect("/inicio");
    }

    /** RF02-CP02 (E2). Se guardan 2 intentos y se redirige a /login?error=credenciales («Usuario o contraseña incorrectos»). */
    @Test
    @DisplayName("RF02-CP02 Ingresar con la contraseña «clave-mala».")
    void rf02_cp02_claveIncorrectaSumaIntento() throws Exception {
        // ARRANGE
        when(usuarioDao.buscarPorUsuario("vendedor")).thenReturn(Optional.of(usuario(2, "vendedor", "VENDEDOR", "ACTIVO", 1)));
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        when(req.getParameter("username")).thenReturn("vendedor");
        when(req.getContextPath()).thenReturn("");
        // ACT
        manejadorAcceso().onAuthenticationFailure(req, resp, new BadCredentialsException("x"));
        // ASSERT
        verify(usuarioDao).actualizarIntentos(2, 2, null);
        verify(resp).sendRedirect("/login?error=credenciales");
    }

    /** RF02-CP03 (E3). El usuario queda bloqueado hasta dentro de 5 minutos y se registra BLOQUEO_ACCESO. */
    @Test
    @DisplayName("RF02-CP03 Ingresar por quinta vez con una contraseña incorrecta.")
    void rf02_cp03_quintoIntentoBloquea() throws Exception {
        // ARRANGE
        when(usuarioDao.buscarPorUsuario("vendedor")).thenReturn(Optional.of(usuario(2, "vendedor", "VENDEDOR", "ACTIVO", 4)));
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        when(req.getParameter("username")).thenReturn("vendedor");
        when(req.getContextPath()).thenReturn("");
        // ACT
        manejadorAcceso().onAuthenticationFailure(req, resp, new BadCredentialsException("x"));
        // ASSERT
        ArgumentCaptor<LocalDateTime> hasta = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(usuarioDao).actualizarIntentos(eq(2), eq(0), hasta.capture());
        long minutos = java.time.Duration.between(Fechas.ahora(), hasta.getValue()).toMinutes();
        assertTrue(minutos >= 4 && minutos <= 5, "el bloqueo debe durar 5 minutos");
        verify(auditoriaDao).registrar(2, "BLOQUEO_ACCESO", "Bloqueado 5 min por intentos fallidos");
    }

    /** RF02-CP04 (E4). Redirección a /login?error=inactivo («Su cuenta está desactivada»); no se cuentan intentos. */
    @Test
    @DisplayName("RF02-CP04 Ingresar con el usuario desactivado.")
    void rf02_cp04_cuentaDesactivada() throws Exception {
        // ARRANGE
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        when(req.getParameter("username")).thenReturn("antiguo");
        when(req.getContextPath()).thenReturn("");
        // ACT
        manejadorAcceso().onAuthenticationFailure(req, resp, new DisabledException("inactivo"));
        // ASSERT
        verify(resp).sendRedirect("/login?error=inactivo");
        verify(usuarioDao, never()).actualizarIntentos(anyInt(), anyInt(), any());
    }
}
