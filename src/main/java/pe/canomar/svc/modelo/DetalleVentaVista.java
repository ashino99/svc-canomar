package pe.canomar.svc.modelo;

import java.math.BigDecimal;
import pe.canomar.svc.comun.Dinero;

/** Línea de una venta. */
public record DetalleVentaVista(int idProducto, String producto, BigDecimal kilos, BigDecimal precioKg, BigDecimal costoKg) {
    public BigDecimal importe() {
        return Dinero.r2(kilos.multiply(precioKg));
    }
}
