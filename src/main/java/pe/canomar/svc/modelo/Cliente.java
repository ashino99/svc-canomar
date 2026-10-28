package pe.canomar.svc.modelo;

import java.math.BigDecimal;

/** Restaurante o cliente frecuente con crédito (tabla cliente). */
public record Cliente(Integer id, String nombreComercial, String ruc, String telefono, BigDecimal limiteCredito, int plazoDias, String estado) {
}
