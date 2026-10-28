package pe.canomar.svc.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.canomar.svc.comun.Fechas;
import pe.canomar.svc.modelo.ItemVenta;
import pe.canomar.svc.modelo.NuevaVenta;
import pe.canomar.svc.modelo.ResultadoSync;
import pe.canomar.svc.seguridad.UsuarioSesion;
import pe.canomar.svc.servicio.VentaService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** API usada por el celular para trabajar sin conexión (RF24–RF26). */
@RestController
@RequestMapping("/api")
public class VentaApiController {

    /** Venta guardada en el celular. */
    public record VentaOffline(String uuid, String fechaHora, String tipo, String medio, Integer idCliente, boolean remate, List<Linea> items) {
    }

    public record Linea(int idProducto, BigDecimal kilos, BigDecimal precio) {
    }

    private final VentaService ventas;

    public VentaApiController(VentaService ventas) {
        this.ventas = ventas;
    }

    /** RF25: recibe las ventas guardadas sin conexión; las repetidas se ignoran por su uuid. */
    @PostMapping("/ventas/sync")
    public List<ResultadoSync> sincronizar(@AuthenticationPrincipal UsuarioSesion yo, @RequestBody List<VentaOffline> pendientes) {
        List<NuevaVenta> lista = pendientes.stream().map(v -> new NuevaVenta(v.uuid(), fecha(v.fechaHora()), v.tipo(), v.medio(), v.idCliente(), v.remate(),
                v.items() == null ? List.of() : v.items().stream().map(l -> new ItemVenta(l.idProducto(), l.kilos(), l.precio())).toList())).toList();
        return ventas.sincronizar(lista, yo.getId());
    }

    private static LocalDateTime fecha(String iso) {
        try {
            LocalDateTime f = LocalDateTime.parse(iso);
            return f.isAfter(Fechas.ahora()) ? Fechas.ahora() : f;
        } catch (RuntimeException e) {
            return Fechas.ahora();
        }
    }
}
