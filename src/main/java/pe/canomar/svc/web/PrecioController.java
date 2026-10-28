package pe.canomar.svc.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.canomar.svc.seguridad.UsuarioSesion;
import pe.canomar.svc.servicio.PrecioService;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** P09 Precios del día: registrar (RF15) y consultar (RF16). */
@Controller
public class PrecioController {

    private final PrecioService precios;

    public PrecioController(PrecioService precios) {
        this.precios = precios;
    }

    @GetMapping("/precios")
    public String ver(Model model) {
        model.addAttribute("precios", precios.preciosDeHoy());
        return "precios";
    }

    @PostMapping("/precios")
    public String guardar(@AuthenticationPrincipal UsuarioSesion yo, @RequestParam(name = "idProducto") List<Integer> ids,
                          @RequestParam(name = "precio") List<BigDecimal> valores,
                          @RequestParam(defaultValue = "false") boolean confirmado, RedirectAttributes ra) {
        Map<Integer, BigDecimal> mapa = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) {
            BigDecimal v = Formularios.en(valores, i);
            if (v != null) {
                mapa.put(ids.get(i), v);
            }
        }
        int n = precios.guardarPrecios(mapa, confirmado, yo.getId());
        ra.addFlashAttribute("ok", "Precios del día guardados · " + n + " productos");
        return "redirect:/precios";
    }

    @PostMapping("/precios/ayer")
    public String usarAyer(@AuthenticationPrincipal UsuarioSesion yo, RedirectAttributes ra) {
        int n = precios.usarPreciosDeAyer(yo.getId());
        ra.addFlashAttribute("ok", n == 0 ? "No había precios por copiar" : "Se mantienen los precios anteriores en " + n + " productos");
        return "redirect:/precios";
    }
}
