package pe.canomar.svc.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import pe.canomar.svc.comun.Fechas;
import pe.canomar.svc.dao.AuditoriaDao;
import pe.canomar.svc.dao.UsuarioDao;

import java.io.IOException;

/**
 * RF02 / RNF18: cuenta los intentos fallidos y bloquea el acceso 5 minutos al quinto intento.
 * Al ingresar correctamente reinicia el contador y registra el acceso en la bitácora.
 */
@Component
public class ManejadorAcceso implements AuthenticationSuccessHandler, AuthenticationFailureHandler {

    public static final int MAX_INTENTOS = 5;
    public static final int MINUTOS_BLOQUEO = 5;
    private static final Logger log = LoggerFactory.getLogger(ManejadorAcceso.class);

    private final UsuarioDao usuarioDao;
    private final AuditoriaDao auditoriaDao;

    public ManejadorAcceso(UsuarioDao usuarioDao, AuditoriaDao auditoriaDao) {
        this.usuarioDao = usuarioDao;
        this.auditoriaDao = auditoriaDao;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication auth) throws IOException {
        UsuarioSesion u = (UsuarioSesion) auth.getPrincipal();
        usuarioDao.actualizarIntentos(u.getId(), 0, null);
        auditoriaDao.registrar(u.getId(), "INICIAR_SESION", "Ingreso de " + u.getUsername());
        log.info("Inicio de sesión: {}", u.getUsername());
        response.sendRedirect(request.getContextPath() + "/inicio");
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) throws IOException {
        String usuario = request.getParameter("username");
        String error = "credenciales";
        if (ex instanceof LockedException) {
            error = "bloqueado";
        } else if (ex instanceof DisabledException) {
            error = "inactivo";
        } else if (usuario != null) {
            usuarioDao.buscarPorUsuario(usuario.trim()).ifPresent(u -> {
                int intentos = u.intentosFallidos() + 1;
                if (intentos >= MAX_INTENTOS) {
                    usuarioDao.actualizarIntentos(u.id(), 0, Fechas.ahora().plusMinutes(MINUTOS_BLOQUEO));
                    auditoriaDao.registrar(u.id(), "BLOQUEO_ACCESO", "Bloqueado " + MINUTOS_BLOQUEO + " min por intentos fallidos");
                    log.warn("Usuario bloqueado por intentos fallidos: {}", u.usuario());
                } else {
                    usuarioDao.actualizarIntentos(u.id(), intentos, null);
                }
            });
        }
        response.sendRedirect(request.getContextPath() + "/login?error=" + error);
    }
}
