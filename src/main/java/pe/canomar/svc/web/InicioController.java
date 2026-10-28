package pe.canomar.svc.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pe.canomar.svc.comun.Fechas;
import pe.canomar.svc.servicio.PanelService;

/** P01 Iniciar sesión, P02 Inicio (panel del negocio) y M1 menú «Más». */
@Controller
public class InicioController {

    private final PanelService panel;

    public InicioController(PanelService panel) {
        this.panel = panel;
    }

    @GetMapping("/")
    public String raiz() {
        return "redirect:/inicio";
    }

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("error", switch (error) {
                case "bloqueado" -> "Acceso bloqueado por 5 minutos por intentos fallidos";
                case "inactivo" -> "Su cuenta está desactivada. Consulte al administrador";
                default -> "Usuario o contraseña incorrectos";
            });
        }
        return "login";
    }

    /** RF46, RF29, RF34: panel del negocio con indicadores y alertas. */
    @GetMapping("/inicio")
    public String inicio(Model model) {
        model.addAttribute("panel", panel.panel());
        model.addAttribute("hoy", Fechas.hoy());
        return "inicio";
    }

    @GetMapping("/mas")
    public String mas() {
        return "mas";
    }

    @GetMapping("/sin-permiso")
    public String sinPermiso() {
        return "sin-permiso";
    }
}
