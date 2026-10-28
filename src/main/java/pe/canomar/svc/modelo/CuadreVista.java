package pe.canomar.svc.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import pe.canomar.svc.comun.Calculos;

/** Cuadre de caja guardado (RF39, RF41). */
public record CuadreVista(int id, LocalDate fecha, BigDecimal efectivoEsperado, BigDecimal efectivoContado, BigDecimal digitalEsperado, BigDecimal digitalRevisado, String motivo, BigDecimal ganancia, String usuario) {
    public BigDecimal diferenciaEfectivo() {
        return Calculos.diferencia(efectivoContado, efectivoEsperado);
    }

    public BigDecimal diferenciaDigital() {
        return Calculos.diferencia(digitalRevisado, digitalEsperado);
    }
}
