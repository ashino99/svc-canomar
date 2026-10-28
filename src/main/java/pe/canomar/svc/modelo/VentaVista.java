package pe.canomar.svc.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Venta con su total y datos para listar. */
public record VentaVista(long id, String uuid, LocalDateTime fechaHora, Integer idCliente, String cliente, String tipoPago, String medioPago, boolean esRemate, String estado, String motivoAnulacion, BigDecimal total, String usuario, Integer idFactura, boolean sincronizada) {
    public boolean anulada() {
        return "ANULADA".equals(estado);
    }

    public String numero() {
        return String.format("%04d", id);
    }
}
