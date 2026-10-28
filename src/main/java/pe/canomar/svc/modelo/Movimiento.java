package pe.canomar.svc.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Línea del estado de cuenta. */
public record Movimiento(LocalDateTime fecha, String descripcion, BigDecimal cargo, BigDecimal abono, BigDecimal saldo) {
}
