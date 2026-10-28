package pe.canomar.svc.modelo;

import java.math.BigDecimal;

/** Monto pendiente de un cliente en un tramo de antigüedad (RF33). */
public record Antiguedad(String tramo, BigDecimal monto, int ventas) {
}
