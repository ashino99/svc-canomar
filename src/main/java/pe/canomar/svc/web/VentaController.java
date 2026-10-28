package pe.canomar.svc.web;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.canomar.svc.comun.Fechas;
import pe.canomar.svc.modelo.ItemVenta;
import pe.canomar.svc.modelo.NuevaVenta;
import pe.canomar.svc.modelo.VentaVista;
import pe.canomar.svc.seguridad.UsuarioSesion;
import pe.canomar.svc.servicio.ClienteService;
import pe.canomar.svc.servicio.CreditoService;
import pe.canomar.svc.servicio.PrecioService;
import pe.canomar.svc.servicio.VentaService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** P10 Vender (RF17–RF19, RF21, RF24–RF26) y P11 Ventas del día (RF20, RF23). */
@Controller
public class VentaController {

    private final VentaService ventas;
    private final PrecioService precios;
    private final ClienteService clientes;
    private final CreditoService credito;

    public VentaController(VentaService ventas, PrecioService precios, ClienteService clientes, CreditoService credito) {
        this.ventas = ventas;
        this.precios = precios;
        this.clientes = clientes;
        this.credito = credito;
    }

    @GetMapping("/vender")
    public String vender(@RequestParam(defaultValue = "false") boolean remate, Model model) {
        model.addAttribute("precios", precios.preciosDeHoy());
        model.addAttribute("clientes", clientes.activos().stream().filter(c -> c.limiteCredito().signum() > 0).toList());
        model.addAttribute("saldos", credito.saldos().stream().collect(Collectors.toMap(s -> s.idCliente(), Function.identity())));
        model.addAttribute("remate", remate);
        return "vender";
    }

    @PostMapping("/vender")
    public String registrar(@AuthenticationPrincipal UsuarioSesion yo, @RequestParam String tipo, @RequestParam(required = false) String medio,
                            @RequestParam(required = false) Integer idCliente, @RequestParam(defaultValue = "false") boolean remate,
                            @RequestParam(required = false) String uuid,
                            @RequestParam(name = "idProducto", required = false) List<Integer> idProducto,
                            @RequestParam(name = "kilos", required = false) List<BigDecimal> kilos,
                            @RequestParam(name = "precio", required = false) List<BigDecimal> precio,
                            @RequestParam(defaultValue = "false") boolean confirmado, RedirectAttributes ra) {
        int n = Formularios.tamano(idProducto);
        List<ItemVenta> items = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (idProducto.get(i) != null) {
                items.add(new ItemVenta(idProducto.get(i), Formularios.en(kilos, i), Formularios.en(precio, i)));
            }
        }
        NuevaVenta nv = new NuevaVenta(uuid, null, tipo, medio, idCliente, remate, items);
        VentaVista v = ventas.registrar(nv, yo.getId(), yo.esAdministrador(), confirmado);
        String detalle = v.esRemate() ? "Remate registrado" : ("CREDITO".equals(v.tipoPago()) ? "Venta al crédito registrada · " + v.cliente() : "Venta registrada");
        String extra = "CREDITO".equals(v.tipoPago()) && v.idCliente() != null
                ? " · nuevo saldo S/ " + credito.saldo(v.idCliente()).saldo()
                : (v.medioPago() != null ? " · " + v.medioPago() : "");
        ra.addFlashAttribute("ok", detalle + " · S/ " + v.total() + extra);
        return remate ? "redirect:/vender?remate=true" : "redirect:/vender";
    }

    @GetMapping("/ventas")
    public String delDia(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha, Model model) {
        LocalDate f = fecha == null ? Fechas.hoy() : fecha;
        List<VentaVista> lista = ventas.ventasDelDia(f);
        List<VentaVista> vigentes = lista.stream().filter(v -> !v.anulada()).toList();
        Map<String, BigDecimal> porTipo = vigentes.stream().collect(Collectors.groupingBy(
                v -> "CREDITO".equals(v.tipoPago()) ? "Crédito" : (v.medioPago() == null ? "Efectivo" : switch (v.medioPago()) {
                    case "YAPE" -> "Yape/Plin";
                    case "TRANSFERENCIA" -> "Transferencia";
                    default -> "Efectivo";
                }),
                java.util.TreeMap::new, Collectors.reducing(BigDecimal.ZERO, VentaVista::total, BigDecimal::add)));
        model.addAttribute("fecha", f);
        model.addAttribute("ventas", lista);
        model.addAttribute("cantidad", vigentes.size());
        model.addAttribute("total", vigentes.stream().map(VentaVista::total).reduce(BigDecimal.ZERO, BigDecimal::add));
        model.addAttribute("porTipo", porTipo);
        return "ventas";
    }

    @GetMapping("/ventas/{id}")
    public String detalle(@PathVariable long id, Model model) {
        model.addAttribute("venta", ventas.buscar(id));
        model.addAttribute("detalle", ventas.detalle(id));
        return "venta-detalle";
    }

    @PostMapping("/ventas/{id}/anular")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String anular(@AuthenticationPrincipal UsuarioSesion yo, @PathVariable long id, @RequestParam String motivo, RedirectAttributes ra) {
        ventas.anular(id, motivo, yo.getId());
        ra.addFlashAttribute("ok", "Venta anulada · los kilos volvieron al saldo");
        return "redirect:/ventas/" + id;
    }
}
