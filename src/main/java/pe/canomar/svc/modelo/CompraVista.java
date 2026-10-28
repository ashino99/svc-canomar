package pe.canomar.svc.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Compra con sus totales calculados. */
public record CompraVista(int id, LocalDate fecha, String proveedor, String tipoPago, BigDecimal kilos, BigDecimal total) {
}
