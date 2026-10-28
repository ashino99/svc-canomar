package pe.canomar.svc.servicio;

import pe.canomar.svc.modelo.CuadreVista;
import pe.canomar.svc.modelo.Ganancia;
import pe.canomar.svc.modelo.GastoVista;
import pe.canomar.svc.modelo.Movimientos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Gastos menores, cuadre de caja y ganancia del día (RF38–RF41). */
public interface CajaService {
    void registrarGasto(String concepto, String descripcion, BigDecimal monto, String medio, int idActor);

    List<GastoVista> gastosDelDia(LocalDate fecha);

    Movimientos movimientos(LocalDate fecha);

    Optional<CuadreVista> cuadre(LocalDate fecha);

    CuadreVista realizarCuadre(LocalDate fecha, BigDecimal efectivoContado, BigDecimal digitalRevisado, String motivo, int idActor);

    Ganancia ganancia(LocalDate fecha);

    List<CuadreVista> historial(LocalDate desde, LocalDate hasta);
}
