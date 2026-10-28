package pe.canomar.svc.modelo;

import java.math.BigDecimal;
import java.time.YearMonth;
import pe.canomar.svc.comun.Calculos;
import pe.canomar.svc.comun.Dinero;

/** Resumen del mes para el contador (RF45). */
public record CierreMensual(YearMonth mes, BigDecimal ventas, BigDecimal costoVendido, BigDecimal compras, BigDecimal gastos, BigDecimal perdidas, BigDecimal facturado, int diasSinCuadre, boolean parcial) {
    public BigDecimal ganancia() {
        return Calculos.ganancia(ventas, costoVendido, perdidas, gastos);
    }

    public BigDecimal noFacturado() {
        return Dinero.r2(Dinero.nz(ventas).subtract(Dinero.nz(facturado)));
    }
}
