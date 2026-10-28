package pe.canomar.svc.web;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContextUtils;
import pe.canomar.svc.comun.ConfirmacionException;
import pe.canomar.svc.comun.NegocioException;
import pe.canomar.svc.seguridad.UsuarioSesion;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Manejo centralizado de mensajes: los errores del negocio vuelven a la pantalla anterior con el mensaje,
 * y las operaciones que requieren confirmación muestran una pantalla para confirmar con los mismos datos.
 */
@ControllerAdvice
public class ManejadorGlobal {

    private static final Logger log = LoggerFactory.getLogger(ManejadorGlobal.class);

    /** Campo oculto de un formulario reenviado al confirmar. */
    public record Campo(String nombre, String valor) {
    }

    @ModelAttribute("yo")
    public UsuarioSesion usuarioActual(@AuthenticationPrincipal UsuarioSesion yo) {
        return yo;
    }

    @ExceptionHandler(NegocioException.class)
    public String negocio(NegocioException e, HttpServletRequest req) {
        RequestContextUtils.getOutputFlashMap(req).put("error", e.getMessage());
        return "redirect:" + volver(req);
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class, NumberFormatException.class})
    public String datosInvalidos(Exception e, HttpServletRequest req) {
        log.debug("Datos inválidos: {}", e.getMessage());
        RequestContextUtils.getOutputFlashMap(req).put("error", "Revise los datos ingresados: hay un campo vacío o con formato incorrecto");
        return "redirect:" + volver(req);
    }

    @ExceptionHandler(ConfirmacionException.class)
    public ModelAndView confirmar(ConfirmacionException e, HttpServletRequest req) {
        List<Campo> campos = new ArrayList<>();
        for (Map.Entry<String, String[]> p : req.getParameterMap().entrySet()) {
            if (p.getKey().equals("_csrf") || p.getKey().equals("confirmado") || p.getKey().equals("_volver")) {
                continue;
            }
            for (String v : p.getValue()) {
                campos.add(new Campo(p.getKey(), v));
            }
        }
        ModelAndView mv = new ModelAndView("confirmar");
        mv.addObject("mensaje", e.getMessage());
        mv.addObject("accion", req.getRequestURI().substring(req.getContextPath().length()));
        mv.addObject("campos", campos);
        mv.addObject("volver", volver(req));
        return mv;
    }

    /** Ruta de la pantalla anterior (solo del mismo sitio), para no redirigir a otro dominio. */
    static String volver(HttpServletRequest req) {
        String propio = req.getParameter("_volver");
        if (propio != null && propio.startsWith("/") && !propio.startsWith("//") && !propio.contains("\\")) {
            return propio;
        }
        String ref = req.getHeader("Referer");
        if (ref != null) {
            try {
                URI u = URI.create(ref);
                if (u.getHost() == null || u.getHost().equals(req.getServerName())) {
                    String path = u.getRawPath();
                    if (path != null && path.startsWith(req.getContextPath() + "/")) {
                        path = path.substring(req.getContextPath().length());
                        return path + (u.getRawQuery() != null ? "?" + u.getRawQuery() : "");
                    }
                }
            } catch (IllegalArgumentException ignored) {
                // Referer mal formado: se vuelve al inicio
            }
        }
        return "/inicio";
    }
}
