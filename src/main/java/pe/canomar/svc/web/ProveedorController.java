package pe.canomar.svc.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.canomar.svc.seguridad.UsuarioSesion;
import pe.canomar.svc.servicio.CompraService;

import java.math.BigDecimal;

/** P07 Proveedores: registrar (RF10), pagar (RF13) y consultar deudas (RF14). Administrador. */
@Controller
@RequestMapping("/proveedores")
public class ProveedorController {

    private final CompraService compras;

    public ProveedorController(CompraService compras) {
        this.compras = compras;
    }

    @GetMapping
    public String ver(@RequestParam(required = false) Integer proveedor, Model model) {
        var deudas = compras.deudas();
        model.addAttribute("deudas", proveedor == null ? deudas : deudas.stream().filter(d -> d.idProveedor() == proveedor).toList());
        model.addAttribute("deudaTotal", deudas.stream().map(d -> d.deuda()).reduce(BigDecimal.ZERO, BigDecimal::add));
        model.addAttribute("proveedores", compras.proveedores());
        model.addAttribute("pagos", compras.pagosRecientes());
        model.addAttribute("filtro", proveedor);
        return "proveedores";
    }

    @PostMapping
    public String registrar(@AuthenticationPrincipal UsuarioSesion yo, @RequestParam String nombre, @RequestParam(required = false) String ruc,
                            @RequestParam String tipo, @RequestParam(required = false) String telefono, RedirectAttributes ra) {
        compras.registrarProveedor(nombre, ruc, tipo, telefono, yo.getId());
        ra.addFlashAttribute("ok", "Proveedor registrado · " + nombre.trim());
        return "redirect:/proveedores";
    }

    @PostMapping("/pago")
    public String pagar(@AuthenticationPrincipal UsuarioSesion yo, @RequestParam int idProveedor, @RequestParam BigDecimal monto,
                        @RequestParam String medio, RedirectAttributes ra) {
        BigDecimal pendiente = compras.registrarPago(idProveedor, monto, medio, yo.getId());
        ra.addFlashAttribute("ok", "Pago registrado · deuda pendiente S/ " + pendiente);
        return "redirect:/proveedores";
    }
}
