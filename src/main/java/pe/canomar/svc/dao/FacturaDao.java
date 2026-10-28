package pe.canomar.svc.dao;

import pe.canomar.svc.modelo.FacturaVista;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Acceso a factura. */
public interface FacturaDao {
    int insertar(int idCliente, String serie, int numero, LocalDate fechaEmision, BigDecimal importe, boolean detraccion);

    boolean existe(String serie, int numero);

    List<FacturaVista> listar(int limite);

    Optional<FacturaVista> buscar(int idFactura);

    BigDecimal totalEmitido(LocalDate desde, LocalDate hasta);
}
