package pe.canomar.svc.modelo;

import java.math.BigDecimal;
import pe.canomar.svc.comun.Dinero;

/** Precio del día de un producto con su costo, el precio anterior y el saldo (RF15, RF16). */
public record PrecioVista(int idProducto, String producto, String nombreCorto, BigDecimal costoKg, BigDecimal precioHoy, BigDecimal precioAnterior, BigDecimal kilosDisponibles) {
    public boolean agotado() {
        return !Dinero.positivo(kilosDisponibles);
    }

    public boolean bajoCosto() {
        return precioHoy != null && costoKg != null && precioHoy.compareTo(costoKg) < 0;
    }

    public BigDecimal precioSugerido() {
        return precioHoy != null ? precioHoy : precioAnterior;
    }
}
