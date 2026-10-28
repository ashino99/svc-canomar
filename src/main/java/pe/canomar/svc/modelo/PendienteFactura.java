package pe.canomar.svc.modelo;

import java.math.BigDecimal;

/** Ventas de un cliente con RUC aún sin factura (RF44). */
public record PendienteFactura(int idCliente, String cliente, String ruc, int ventas, BigDecimal total) {
}
