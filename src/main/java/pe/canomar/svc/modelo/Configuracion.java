package pe.canomar.svc.modelo;

import java.math.BigDecimal;

/** Parámetros del negocio (tabla configuracion). */
public record Configuracion(int diasAlertaSaldo, BigDecimal toleranciaConsumo, BigDecimal toleranciaCuadre) {
}
