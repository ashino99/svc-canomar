package pe.canomar.svc.modelo;

import java.math.BigDecimal;

/** Producto ingresado en una compra. */
public record ItemCompra(int idProducto, BigDecimal kilos, BigDecimal costoTotal) {
}
