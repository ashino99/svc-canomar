package pe.canomar.svc.modelo;

import java.math.BigDecimal;
import pe.canomar.svc.comun.Calculos;

/** Producto vendido: kilos de la balanza y precio aplicado. */
public record ItemVenta(int idProducto, BigDecimal kilos, BigDecimal precioKg) {
    public BigDecimal importe() {
        return Calculos.importe(kilos, precioKg);
    }
}
