package pe.canomar.svc.modelo;

import java.math.BigDecimal;
import pe.canomar.svc.comun.Dinero;

/** Montos del día por medio de pago, base para el cuadre (RF39). */
public record Movimientos(BigDecimal ventasEfectivo, BigDecimal ventasDigital, BigDecimal abonosEfectivo, BigDecimal abonosDigital, BigDecimal gastosEfectivo, BigDecimal gastosDigital, BigDecimal pagosEfectivo, BigDecimal pagosDigital) {
    public BigDecimal efectivoEsperado() {
        return Dinero.r2(ventasEfectivo.add(abonosEfectivo).subtract(gastosEfectivo).subtract(pagosEfectivo));
    }

    public BigDecimal digitalEsperado() {
        return Dinero.r2(ventasDigital.add(abonosDigital).subtract(gastosDigital).subtract(pagosDigital));
    }
}
