package pe.canomar.svc.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Datos de una venta por registrar (desde el formulario o desde el celular sin conexión). */
public record NuevaVenta(String uuid, LocalDateTime fechaHora, String tipoPago, String medioPago, Integer idCliente, boolean esRemate, List<ItemVenta> items) {
    public BigDecimal total() {
        return items.stream().map(ItemVenta::importe).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean credito() {
        return "CREDITO".equals(tipoPago);
    }
}
