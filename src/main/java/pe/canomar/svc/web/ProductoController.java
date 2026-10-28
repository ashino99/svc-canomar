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
import pe.canomar.svc.servicio.ProductoService;

/** P05 Productos: registrar (RF06) y editar (RF07). */
@Controller
@RequestMapping("/productos")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class ProductoController {

    private final ProductoService productos;

    public ProductoController(ProductoService productos) {
        this.productos = productos;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) Integer editar, Model model) {
        model.addAttribute("productos", productos.listar());
        if (editar != null) {
            model.addAttribute("editando", productos.buscar(editar));
        }
        return "productos";
    }

    @PostMapping
    public String registrar(@AuthenticationPrincipal UsuarioSesion yo, @RequestParam String nombre, @RequestParam(required = false) String nombreCorto,
                            @RequestParam String categoria, RedirectAttributes ra) {
        productos.registrar(nombre, nombreCorto, categoria, yo.getId());
        ra.addFlashAttribute("ok", "Producto registrado · " + nombre.trim());
        return "redirect:/productos";
    }

    @PostMapping("/{id}")
    public String editar(@AuthenticationPrincipal UsuarioSesion yo, @PathVariable int id, @RequestParam String nombre,
                         @RequestParam(required = false) String nombreCorto, @RequestParam String categoria, @RequestParam String estado,
                         RedirectAttributes ra) {
        productos.editar(id, nombre, nombreCorto, categoria, estado, yo.getId());
        ra.addFlashAttribute("ok", "Producto actualizado");
        return "redirect:/productos";
    }
}
