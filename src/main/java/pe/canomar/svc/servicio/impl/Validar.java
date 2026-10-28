package pe.canomar.svc.servicio.impl;

import pe.canomar.svc.comun.Dinero;
import pe.canomar.svc.comun.NegocioException;

import java.math.BigDecimal;
import java.util.Set;

/** Validaciones repetidas en varios servicios. */
final class Validar {
    static final Set<String> MEDIOS = Set.of("EFECTIVO", "YAPE", "TRANSFERENCIA");

    private Validar() {
    }

    static BigDecimal montoPositivo(BigDecimal monto, String mensaje) {
        if (!Dinero.positivo(monto)) {
            throw new NegocioException(mensaje);
        }
        return Dinero.r2(monto);
    }

    static String medio(String medio) {
        if (medio == null || !MEDIOS.contains(medio)) {
            throw new NegocioException("Elija el medio de pago");
        }
        return medio;
    }
}
