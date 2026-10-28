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
import pe.canomar.svc.servicio.ClienteService;
import pe.canomar.svc.servicio.CreditoService;

import java.math.BigDecimal;

/** P06 Clientes: registrar (RF08), editar (RF09) y consultar saldo (RF32). */
@Controller
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteService clientes;
    private final CreditoService credito;

    public ClienteController(ClienteService clientes, CreditoService credito) {
        this.clientes = clientes;
        this.credito = credito;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) Integer editar, Model model) {
        model.addAttribute("saldos", credito.saldos());
        model.addAttribute("clientes", clientes.listar());
        if (editar != null) {
            model.addAttribute("editando", clientes.buscar(editar));
        }
        return "clientes";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable int id, Model model) {
        model.addAttribute("cliente", clientes.buscar(id));
        model.addAttribute("saldo", credito.saldo(id));
        model.addAttribute("pendientes", credito.pendientes(id));
        return "cliente-detalle";
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String registrar(@AuthenticationPrincipal UsuarioSesion yo, @RequestParam String nombre, @RequestParam(required = false) String ruc,
                            @RequestParam(required = false) String telefono, @RequestParam BigDecimal limite, @RequestParam int plazo,
                            RedirectAttributes ra) {
        clientes.registrar(nombre, ruc, telefono, limite, plazo, yo.getId());
        ra.addFlashAttribute("ok", "Cliente registrado · " + nombre.trim());
        return "redirect:/clientes";
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String editar(@AuthenticationPrincipal UsuarioSesion yo, @PathVariable int id, @RequestParam String nombre,
                         @RequestParam(required = false) String ruc, @RequestParam(required = false) String telefono,
                         @RequestParam BigDecimal limite, @RequestParam int plazo, @RequestParam String estado, RedirectAttributes ra) {
        String aviso = clientes.editar(id, nombre, ruc, telefono, limite, plazo, estado, yo.getId());
        ra.addFlashAttribute("ok", "Cliente actualizado");
        if (aviso != null) {
            ra.addFlashAttribute("aviso", aviso);
        }
        return "redirect:/clientes";
    }
}
