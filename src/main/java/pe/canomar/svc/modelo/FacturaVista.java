package pe.canomar.svc.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import pe.canomar.svc.comun.Dinero;

/** Factura emitida en SUNAT y lo asociado de ventas (RF42, RF43). */
public record FacturaVista(int id, int idCliente, String cliente, String serie, int numero, LocalDate fechaEmision, BigDecimal importe, boolean detraccion, BigDecimal asociado) {
    public BigDecimal diferencia() {
        return Dinero.r2(Dinero.nz(importe).subtract(Dinero.nz(asociado)));
    }

    public String comprobante() {
        return serie + "-" + String.format("%06d", numero);
    }
}
