package pe.canomar.svc.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.canomar.svc.modelo.ItemCompra;
import pe.canomar.svc.seguridad.UsuarioSesion;
import pe.canomar.svc.servicio.CompraService;
import pe.canomar.svc.servicio.ProductoService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** P08 Nueva compra (RF11) con recálculo del costo promedio (RF12). */
@Controller
public class CompraController {

    private final CompraService compras;
    private final ProductoService productos;

    public CompraController(CompraService compras, ProductoService productos) {
        this.compras = compras;
        this.productos = productos;
    }

    @GetMapping("/compras/nueva")
    public String nueva(Model model) {
        model.addAttribute("proveedores", compras.proveedores());
        model.addAttribute("productos", productos.activos());
        model.addAttribute("comprasHoy", compras.comprasDeHoy());
        return "compra-nueva";
    }

    @PostMapping("/compras")
    public String registrar(@AuthenticationPrincipal UsuarioSesion yo, @RequestParam int idProveedor, @RequestParam String tipoPago,
                            @RequestParam(name = "idProducto", required = false) List<Integer> idProducto,
                            @RequestParam(name = "kilos", required = false) List<BigDecimal> kilos,
                            @RequestParam(name = "costo", required = false) List<BigDecimal> costo, RedirectAttributes ra) {
        int n = Formularios.tamano(idProducto);
        List<ItemCompra> items = new ArrayList<>();
        BigDecimal totalKg = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < n; i++) {
            if (idProducto.get(i) == null) {
                continue;
            }
            ItemCompra it = new ItemCompra(idProducto.get(i), Formularios.en(kilos, i), Formularios.en(costo, i));
            items.add(it);
            totalKg = totalKg.add(it.kilos() == null ? BigDecimal.ZERO : it.kilos());
            total = total.add(it.costoTotal() == null ? BigDecimal.ZERO : it.costoTotal());
        }
        compras.registrarCompra(idProveedor, tipoPago, items, yo.getId());
        ra.addFlashAttribute("ok", "Compra registrada · " + totalKg + " kg · S/ " + total + ("CREDITO".equals(tipoPago) ? " a crédito" : " al contado")
                + ". El costo promedio se actualizó en Precios del día");
        return "redirect:/compras/nueva";
    }
}
