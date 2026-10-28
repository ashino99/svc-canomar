package pe.canomar.svc.comun;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Operaciones con montos y kilos: siempre BigDecimal redondeado a 2 decimales. */
public final class Dinero {
    public static final BigDecimal CERO = new BigDecimal("0.00");
    public static final BigDecimal CIEN = new BigDecimal("100");

    private Dinero() {
    }

    public static BigDecimal r2(BigDecimal valor) {
        return nz(valor).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal nz(BigDecimal valor) {
        return valor == null ? CERO : valor;
    }

    public static boolean positivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }

    public static boolean mayor(BigDecimal a, BigDecimal b) {
        return nz(a).compareTo(nz(b)) > 0;
    }
}
