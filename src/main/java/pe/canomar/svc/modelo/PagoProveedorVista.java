package pe.canomar.svc.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Pago registrado a un proveedor. */
public record PagoProveedorVista(int id, LocalDate fecha, String proveedor, BigDecimal monto, String medio) {
}
