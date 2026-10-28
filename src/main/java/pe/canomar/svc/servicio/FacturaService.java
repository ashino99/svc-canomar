package pe.canomar.svc.servicio;

import pe.canomar.svc.modelo.FacturaVista;
import pe.canomar.svc.modelo.PendienteFactura;
import pe.canomar.svc.modelo.VentaVista;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/** Registro de facturas emitidas en SUNAT y su relación con las ventas (RF42–RF44). */
public interface FacturaService {
    int registrar(int idCliente, String serie, int numero, LocalDate fechaEmision, BigDecimal importe, boolean detraccion, int idActor);

    /** Asocia ventas a la factura; pide confirmación si la suma difiere del importe. */
    BigDecimal asociar(int idFactura, List<Long> idsVenta, boolean confirmado, int idActor);

    List<FacturaVista> listar();

    FacturaVista buscar(int idFactura);

    List<VentaVista> ventasSinFactura(int idCliente);

    List<PendienteFactura> pendientes(YearMonth mes);
}
