package pe.canomar.svc.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Estado de cuenta de un cliente en un periodo (RF36). */
public record EstadoCuenta(Cliente cliente, LocalDate desde, LocalDate hasta, BigDecimal saldoInicial, List<Movimiento> movimientos, BigDecimal saldoFinal) {
}
