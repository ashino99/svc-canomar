package pe.canomar.svc.web;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.canomar.svc.comun.Fechas;
import pe.canomar.svc.modelo.FacturaVista;
import pe.canomar.svc.modelo.PendienteFactura;
import pe.canomar.svc.seguridad.UsuarioSesion;
import pe.canomar.svc.servicio.ClienteService;
import pe.canomar.svc.servicio.FacturaService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/** P18 Facturas: registrar (RF42), asociar ventas (RF43) y pendientes de facturar (RF44). Contador. */
@Controller
@RequestMapping("/facturas")
public class FacturaController {

    private final FacturaService facturas;
    private final ClienteService clientes;

    public FacturaController(FacturaService facturas, ClienteService clientes) {
        this.facturas = facturas;
        this.clientes = clientes;
    }

    @GetMapping
    public String ver(@RequestParam(required = false) YearMonth mes, Model model) {
        YearMonth m = mes == null ? YearMonth.from(Fechas.hoy()) : mes;
        List<PendienteFactura> pend = facturas.pendientes(m);
        model.addAttribute("mes", m);
        model.addAttribute("facturas", facturas.listar());
        model.addAttribute("pendientes", pend);
        model.addAttribute("totalPendiente", pend.stream().map(PendienteFactura::total).reduce(BigDecimal.ZERO, BigDecimal::add));
        model.addAttribute("clientes", clientes.activos().stream().filter(c -> c.ruc() != null).toList());
        model.addAttribute("hoy", Fechas.hoy());
        return "facturas";
    }

    @PostMapping
    public String registrar(@AuthenticationPrincipal UsuarioSesion yo, @RequestParam int idCliente, @RequestParam String serie, @RequestParam int numero,
                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha, @RequestParam BigDecimal importe,
                            @RequestParam(defaultValue = "false") boolean detraccion, RedirectAttributes ra) {
        int id = facturas.registrar(idCliente, serie, numero, fecha, importe, detraccion, yo.getId());
        ra.addFlashAttribute("ok", "Factura registrada · ahora asocie las ventas que sustenta");
        return "redirect:/facturas/" + id;
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable int id, Model model) {
        FacturaVista f = facturas.buscar(id);
        model.addAttribute("factura", f);
        model.addAttribute("ventas", facturas.ventasSinFactura(f.idCliente()));
        return "factura-detalle";
    }

    @PostMapping("/{id}/asociar")
    public String asociar(@AuthenticationPrincipal UsuarioSesion yo, @PathVariable int id, @RequestParam(name = "venta", required = false) List<Long> ventas,
                          @RequestParam(defaultValue = "false") boolean confirmado, RedirectAttributes ra) {
        BigDecimal dif = facturas.asociar(id, ventas, confirmado, yo.getId());
        ra.addFlashAttribute("ok", (ventas == null ? 0 : ventas.size()) + " ventas asociadas" + (dif.signum() == 0 ? " · sin diferencia" : " · diferencia S/ " + dif));
        return "redirect:/facturas/" + id;
    }
}
