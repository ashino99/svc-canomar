package pe.canomar.svc.modelo;

import java.math.BigDecimal;
import pe.canomar.svc.comun.Calculos;
import pe.canomar.svc.comun.Dinero;

/** Ganancia de un producto en un periodo (RF47). */
public record GananciaProducto(String producto, BigDecimal kilos, BigDecimal ventas, BigDecimal costo) {
    public BigDecimal ganancia() {
        return Dinero.r2(Dinero.nz(ventas).subtract(Dinero.nz(costo)));
    }

    public BigDecimal margen() {
        return Calculos.margen(ganancia(), ventas);
    }
}
