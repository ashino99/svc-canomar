package pe.canomar.svc.web;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import pe.canomar.svc.modelo.EstadoCuenta;
import pe.canomar.svc.modelo.SaldoCliente;
import pe.canomar.svc.seguridad.UsuarioSesion;
import pe.canomar.svc.servicio.ClienteService;
import pe.canomar.svc.servicio.CreditoService;
import pe.canomar.svc.servicio.ReporteService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** P14 Cobranza (RF31, RF33–RF35) y P15 Estado de cuenta (RF36, RF37). Administrador. */
@Controller
@RequestMapping("/cobranza")
public class CobranzaController {

    private final CreditoService credito;
    private final ClienteService clientes;
    private final ReporteService reportes;

    public CobranzaController(CreditoService credito, ClienteService clientes, ReporteService reportes) {
        this.credito = credito;
        this.clientes = clientes;
        this.reportes = reportes;
    }

    @GetMapping
    public String ver(Model model) {
        List<SaldoCliente> saldos = credito.saldos();
        model.addAttribute("saldos", saldos.stream().filter(s -> s.saldo().signum() > 0).toList());
        model.addAttribute("porCobrar", saldos.stream().map(SaldoCliente::saldo).reduce(BigDecimal.ZERO, BigDecimal::add));
        model.addAttribute("vencido", saldos.stream().map(SaldoCliente::vencido).reduce(BigDecimal.ZERO, BigDecimal::add));
        model.addAttribute("compromisos", credito.compromisos());
        return "cobranza";
    }

    @GetMapping("/{id}")
    public String cliente(@PathVariable int id, Model model) {
        model.addAttribute("cliente", clientes.buscar(id));
        model.addAttribute("saldo", credito.saldo(id));
        model.addAttribute("antiguedad", credito.antiguedad(id));
        model.addAttribute("pendientes", credito.pendientes(id).stream().filter(p -> p.pendiente().signum() > 0).toList());
        model.addAttribute("hoy", Fechas.hoy());
        return "cobranza-cliente";
    }

    @PostMapping("/{id}/abono")
    public String abono(@AuthenticationPrincipal UsuarioSesion yo, @PathVariable int id, @RequestParam BigDecimal monto,
                        @RequestParam String medio, RedirectAttributes ra) {
        BigDecimal nuevo = credito.registrarAbono(id, monto, medio, yo.getId());
        ra.addFlashAttribute("ok", nuevo.signum() == 0 ? "Abono registrado · cliente al día" : "Abono registrado · nuevo saldo S/ " + nuevo);
        return "redirect:/cobranza/" + id;
    }

    @PostMapping("/{id}/compromiso")
    public String compromiso(@AuthenticationPrincipal UsuarioSesion yo, @PathVariable int id,
                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha, @RequestParam BigDecimal monto,
                             RedirectAttributes ra) {
        credito.registrarCompromiso(id, fecha, monto, yo.getId());
        ra.addFlashAttribute("ok", "Compromiso registrado · recordatorio el " + fecha.format(Fechas.DIA));
        return "redirect:/cobranza/" + id;
    }

    @GetMapping("/{id}/estado")
    public String estado(@PathVariable int id, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta, Model model) {
        LocalDate h = hasta == null ? Fechas.hoy() : hasta;
        LocalDate d = desde == null ? h.withDayOfMonth(1) : desde;
        model.addAttribute("estado", credito.estadoCuenta(id, d, h));
        return "estado-cuenta";
    }

    @GetMapping("/{id}/estado.pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable int id, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        EstadoCuenta ec = credito.estadoCuenta(id, desde, hasta);
        byte[] pdf = reportes.pdfEstadoCuenta(ec);
        String nombre = "EstadoCuenta_" + ec.cliente().nombreComercial().replaceAll("[^A-Za-z0-9]+", "_") + "_" + hasta + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(nombre).build().toString())
                .contentType(MediaType.APPLICATION_PDF).body(pdf);
    }
}
