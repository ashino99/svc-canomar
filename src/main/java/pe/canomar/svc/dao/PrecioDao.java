package pe.canomar.svc.dao;

import pe.canomar.svc.modelo.PrecioVista;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Precio de venta y costo promedio por producto y día (precio_dia). */
public interface PrecioDao {
    List<PrecioVista> preciosDe(LocalDate fecha);

    Optional<BigDecimal> precioDe(int idProducto, LocalDate fecha);

    Optional<BigDecimal> ultimoPrecio(int idProducto, LocalDate antesDe);

    Optional<BigDecimal> ultimoCosto(int idProducto, LocalDate hasta);

    void guardarPrecio(int idProducto, LocalDate fecha, BigDecimal precioKg, BigDecimal costoKg);

    void guardarCosto(int idProducto, LocalDate fecha, BigDecimal costoKg, BigDecimal precioPorDefecto);
}
