package pe.canomar.svc.servicio;

import pe.canomar.svc.modelo.PrecioVista;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Precios del día y costo promedio por kilo (RF12, RF15, RF16). */
public interface PrecioService {
    List<PrecioVista> preciosDeHoy();

    /** Guarda los precios de hoy; pide confirmación si alguno queda por debajo del costo. */
    int guardarPrecios(Map<Integer, BigDecimal> precios, boolean confirmado, int idActor);

    int usarPreciosDeAyer(int idActor);

    /** Recalcula y guarda el costo promedio de hoy del producto (RF12). */
    BigDecimal recalcularCosto(int idProducto);
}
