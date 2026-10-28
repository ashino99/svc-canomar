package pe.canomar.svc.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.canomar.svc.seguridad.UsuarioSesion;
import pe.canomar.svc.servicio.UsuarioService;

/** P04 Mi cuenta: cambiar contraseña (RF04) y cerrar sesión (RF03). */
@Controller
public class CuentaController {

    private final UsuarioService usuarios;

    public CuentaController(UsuarioService usuarios) {
        this.usuarios = usuarios;
    }

    @GetMapping("/mi-cuenta")
    public String miCuenta() {
        return "mi-cuenta";
    }

    @PostMapping("/mi-cuenta/clave")
    public String cambiarClave(@AuthenticationPrincipal UsuarioSesion yo, @RequestParam String actual, @RequestParam String nueva,
                               @RequestParam String repetir, RedirectAttributes ra) {
        usuarios.cambiarClave(yo.getId(), actual, nueva, repetir);
        ra.addFlashAttribute("ok", "Contraseña actualizada");
        return "redirect:/mi-cuenta";
    }
}
