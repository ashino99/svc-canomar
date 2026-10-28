package pe.canomar.svc.web;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.canomar.svc.comun.Fechas;
import pe.canomar.svc.modelo.CuadreVista;
import pe.canomar.svc.seguridad.UsuarioSesion;
import pe.canomar.svc.servicio.CajaService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** P16 Gastos y cuadre (RF38, RF39) y P17 Ganancia del día e historial (RF40, RF41). */
@Controller
public class CajaController {

    private final CajaService caja;

    public CajaController(CajaService caja) {
        this.caja = caja;
    }

    @GetMapping("/cuadre")
    @PreAuthorize("hasAnyRole('VENDEDOR','ADMINISTRADOR')")
    public String ver(Model model) {
        LocalDate hoy = Fechas.hoy();
        model.addAttribute("gastos", caja.gastosDelDia(hoy));
        model.addAttribute("totalGastos", caja.gastosDelDia(hoy).stream().map(g -> g.monto()).reduce(BigDecimal.ZERO, BigDecimal::add));
        model.addAttribute("mov", caja.movimientos(hoy));
        model.addAttribute("cuadre", caja.cuadre(hoy).orElse(null));
        model.addAttribute("hoy", hoy);
        return "cuadre";
    }

    @PostMapping("/cuadre/gasto")
    @PreAuthorize("hasAnyRole('VENDEDOR','ADMINISTRADOR')")
    public String gasto(@AuthenticationPrincipal UsuarioSesion yo, @RequestParam String concepto, @RequestParam(required = false) String descripcion,
                        @RequestParam BigDecimal monto, @RequestParam String medio, RedirectAttributes ra) {
        caja.registrarGasto(concepto, descripcion, monto, medio, yo.getId());
        ra.addFlashAttribute("ok", "Gasto registrado · S/ " + monto);
        return "redirect:/cuadre";
    }

    @PostMapping("/cuadre")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String cuadrar(@AuthenticationPrincipal UsuarioSesion yo, @RequestParam BigDecimal efectivo, @RequestParam BigDecimal digital,
                          @RequestParam(required = false) String motivo, RedirectAttributes ra) {
        CuadreVista c = caja.realizarCuadre(Fechas.hoy(), efectivo, digital, motivo, yo.getId());
        ra.addFlashAttribute("ok", "Cuadre guardado · diferencia S/ " + c.diferenciaEfectivo() + " · ganancia del día S/ " + c.ganancia());
        return "redirect:/cuadre";
    }

    @GetMapping("/ganancia")
    public String ganancia(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta, Model model) {
        LocalDate f = fecha == null ? Fechas.hoy() : fecha;
        LocalDate h = hasta == null ? Fechas.hoy() : hasta;
        LocalDate d = desde == null ? h.minusDays(6) : desde;
        List<CuadreVista> historial = caja.historial(d, h);
        model.addAttribute("g", caja.ganancia(f));
        model.addAttribute("historial", historial);
        model.addAttribute("desde", d);
        model.addAttribute("hasta", h);
        BigDecimal suma = historial.stream().map(CuadreVista::ganancia).reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("sumaHistorial", suma);
        model.addAttribute("promedio", historial.isEmpty() ? BigDecimal.ZERO
                : suma.divide(BigDecimal.valueOf(historial.size()), 2, java.math.RoundingMode.HALF_UP));
        return "ganancia";
    }
}
