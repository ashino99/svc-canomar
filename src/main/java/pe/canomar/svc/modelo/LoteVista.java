package pe.canomar.svc.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Lote de saldo de mercadería con su fecha de ingreso. */
public record LoteVista(int idLote, int idProducto, String producto, LocalDate fechaIngreso, BigDecimal kilos, BigDecimal costoKg) {
}
