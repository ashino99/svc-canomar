package pe.canomar.svc.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.canomar.svc.modelo.SaldoLinea;
import pe.canomar.svc.seguridad.UsuarioSesion;
import pe.canomar.svc.servicio.ProductoService;
import pe.canomar.svc.servicio.SaldoService;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** P12 Saldo (RF22, RF28–RF30) y P13 Cierre del puesto (RF27). */
@Controller
public class SaldoController {

    private final SaldoService saldo;
    private final ProductoService productos;

    public SaldoController(SaldoService saldo, ProductoService productos) {
        this.saldo = saldo;
        this.productos = productos;
    }

    @GetMapping("/saldo")
    public String ver(Model model) {
        List<SaldoLinea> lineas = saldo.saldo();
        model.addAttribute("lineas", lineas);
        model.addAttribute("total", lineas.stream().map(l -> l.lote().kilos()).reduce(BigDecimal.ZERO, BigDecimal::add));
        model.addAttribute("antiguo", saldo.mercaderiaAntigua().stream().map(l -> l.lote().kilos()).reduce(BigDecimal.ZERO, BigDecimal::add));
        model.addAttribute("productos", productos.activos());
        return "saldo";
    }

    @PostMapping("/saldo/autoconsumo")
    public String autoconsumo(@AuthenticationPrincipal UsuarioSesion yo, @RequestParam int idProducto, @RequestParam BigDecimal kilos, RedirectAttributes ra) {
        BigDecimal valor = saldo.registrarAutoconsumo(idProducto, kilos, yo.getId());
        ra.addFlashAttribute("ok", "Autoconsumo registrado · " + kilos + " kg · S/ " + valor + " al costo");
        return "redirect:/saldo";
    }

    @PostMapping("/saldo/ajuste")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String ajuste(@AuthenticationPrincipal UsuarioSesion yo, @RequestParam int idProducto, @RequestParam BigDecimal kilos,
                         @RequestParam String tipo, @RequestParam String motivo, RedirectAttributes ra) {
        BigDecimal valor = saldo.registrarAjuste(idProducto, kilos, tipo, motivo, yo.getId());
        ra.addFlashAttribute("ok", "Ajuste registrado · " + (valor.signum() >= 0 ? "pérdida S/ " + valor : "ingreso S/ " + valor.negate()));
        return "redirect:/saldo";
    }

    @GetMapping("/saldo/cierre")
    public String cierre(Model model) {
        model.addAttribute("productos", productos.activos());
        model.addAttribute("esperados", saldo.kilosEsperados());
        return "cierre-puesto";
    }

    @PostMapping("/saldo/cierre")
    public String guardarCierre(@AuthenticationPrincipal UsuarioSesion yo, @RequestParam(name = "idProducto") List<Integer> ids,
                                @RequestParam(name = "real", required = false) List<BigDecimal> reales,
                                @RequestParam(name = "motivo", required = false) List<String> motivos, RedirectAttributes ra) {
        Map<Integer, BigDecimal> r = new HashMap<>();
        Map<Integer, String> m = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) {
            BigDecimal v = Formularios.en(reales, i);
            if (v != null) {
                r.put(ids.get(i), v);
                m.put(ids.get(i), Formularios.en(motivos, i));
            }
        }
        BigDecimal total = saldo.registrarCierre(r, m, yo.getId());
        ra.addFlashAttribute("ok", "Saldo guardado para mañana · " + total + " kg en " + r.size() + " productos");
        return "redirect:/saldo";
    }
}
