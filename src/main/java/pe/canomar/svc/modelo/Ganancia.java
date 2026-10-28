package pe.canomar.svc.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import pe.canomar.svc.comun.Calculos;

/** Ganancia de un día (RF40). */
public record Ganancia(LocalDate fecha, int ventasCantidad, BigDecimal ventas, BigDecimal costo, BigDecimal perdidas, BigDecimal gastos, boolean cuadrado) {
    public BigDecimal ganancia() {
        return Calculos.ganancia(ventas, costo, perdidas, gastos);
    }

    public BigDecimal margen() {
        return Calculos.margen(ganancia(), ventas);
    }

    public boolean provisional() {
        return !cuadrado;
    }
}
