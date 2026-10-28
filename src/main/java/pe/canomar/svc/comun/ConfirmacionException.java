package pe.canomar.svc.comun;

/**
 * La operación es válida pero necesita que el usuario la confirme
 * (por ejemplo: precio por debajo del costo, venta que supera el saldo o el límite de crédito).
 */
public class ConfirmacionException extends RuntimeException {
    public ConfirmacionException(String mensaje) {
        super(mensaje);
    }
}
