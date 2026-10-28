package pe.canomar.svc.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.canomar.svc.seguridad.UsuarioSesion;
import pe.canomar.svc.servicio.UsuarioService;

/** P03 Usuarios: registrar (RF01) y desactivar (RF05). Solo el administrador. */
@Controller
@RequestMapping("/usuarios")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class UsuarioController {

    private final UsuarioService usuarios;

    public UsuarioController(UsuarioService usuarios) {
        this.usuarios = usuarios;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("usuarios", usuarios.listar());
        return "usuarios";
    }

    @PostMapping
    public String registrar(@AuthenticationPrincipal UsuarioSesion yo, @RequestParam String nombre, @RequestParam String usuario,
                            @RequestParam String rol, @RequestParam String clave, RedirectAttributes ra) {
        usuarios.registrar(nombre, usuario, rol, clave, yo.getId());
        ra.addFlashAttribute("ok", "Usuario creado · " + usuario.trim().toLowerCase());
        return "redirect:/usuarios";
    }

    @PostMapping("/{id}/desactivar")
    public String desactivar(@AuthenticationPrincipal UsuarioSesion yo, @PathVariable int id, RedirectAttributes ra) {
        usuarios.desactivar(id, yo.getId());
        ra.addFlashAttribute("ok", "Usuario desactivado; sus registros se conservan");
        return "redirect:/usuarios";
    }

    @PostMapping("/{id}/activar")
    public String activar(@AuthenticationPrincipal UsuarioSesion yo, @PathVariable int id, RedirectAttributes ra) {
        usuarios.activar(id, yo.getId());
        ra.addFlashAttribute("ok", "Usuario activado");
        return "redirect:/usuarios";
    }
}
