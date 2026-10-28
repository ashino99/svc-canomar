package pe.canomar.svc.servicio;

import pe.canomar.svc.modelo.SaldoLinea;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Saldo de mercadería por lotes, antigüedad, autoconsumo, ajustes y cierre del puesto (RF22, RF27–RF30). */
public interface SaldoService {
    List<SaldoLinea> saldo();

    List<SaldoLinea> mercaderiaAntigua();

    BigDecimal registrarAutoconsumo(int idProducto, BigDecimal kilos, int idActor);

    BigDecimal registrarAjuste(int idProducto, BigDecimal kilos, String tipo, String motivo, int idActor);

    /** Kilos que deberían quedar por producto (idProducto → kilos). */
    Map<Integer, BigDecimal> kilosEsperados();

    /** RF27: guarda lo que realmente sobró; exige motivo si la diferencia supera la tolerancia. */
    BigDecimal registrarCierre(Map<Integer, BigDecimal> kilosReales, Map<Integer, String> motivos, int idActor);
}
