package pe.canomar.svc.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Compromiso de pago de un cliente (RF35). */
public record CompromisoVista(int id, int idCliente, String cliente, LocalDate fecha, BigDecimal monto, String estado) {
}
