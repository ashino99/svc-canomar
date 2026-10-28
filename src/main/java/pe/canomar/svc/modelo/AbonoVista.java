package pe.canomar.svc.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Abono registrado. */
public record AbonoVista(int id, LocalDateTime fecha, String cliente, BigDecimal monto, String medio, String estado) {
}
