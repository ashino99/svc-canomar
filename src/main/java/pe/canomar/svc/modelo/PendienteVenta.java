package pe.canomar.svc.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import pe.canomar.svc.comun.Dinero;

/** Venta al crédito con lo pagado y lo pendiente. */
public record PendienteVenta(long idVenta, LocalDateTime fechaHora, BigDecimal total, BigDecimal pagado) {
    public BigDecimal pendiente() {
        return Dinero.r2(Dinero.nz(total).subtract(Dinero.nz(pagado)));
    }
}
