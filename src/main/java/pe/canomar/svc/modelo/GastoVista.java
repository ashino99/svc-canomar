package pe.canomar.svc.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Gasto menor del día (RF38). */
public record GastoVista(int id, LocalDate fecha, String concepto, String descripcion, BigDecimal monto, String medio, String usuario) {
}
