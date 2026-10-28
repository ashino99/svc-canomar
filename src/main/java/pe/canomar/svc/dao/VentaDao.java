package pe.canomar.svc.dao;

import pe.canomar.svc.modelo.DetalleVentaVista;
import pe.canomar.svc.modelo.GananciaProducto;
import pe.canomar.svc.modelo.NuevaVenta;
import pe.canomar.svc.modelo.PendienteFactura;
import pe.canomar.svc.modelo.PendienteVenta;
import pe.canomar.svc.modelo.SaldoCliente;
import pe.canomar.svc.modelo.VentaVista;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Acceso a venta y detalle_venta, y consultas de créditos y ganancias. */
public interface VentaDao {
    long insertarVenta(NuevaVenta venta, int idUsuario, LocalDateTime fechaSincronizacion);

    void insertarDetalle(long idVenta, int idProducto, BigDecimal kilos, BigDecimal precioKg, BigDecimal costoKg);

    boolean existeUuid(String uuid);

    Optional<VentaVista> buscar(long idVenta);

    List<DetalleVentaVista> detalle(long idVenta);

    List<VentaVista> listarEntre(LocalDate desde, LocalDate hasta);

    void anular(long idVenta, String motivo);

    /** Ventas al crédito vigentes de un cliente con lo pagado, de la más antigua a la más reciente. */
    List<PendienteVenta> pendientesCliente(int idCliente);

    List<SaldoCliente> saldosClientes(LocalDate hoy);

    List<VentaVista> ventasCreditoCliente(int idCliente, LocalDateTime desde, LocalDateTime hasta);

    BigDecimal ventasCreditoAntesDe(int idCliente, LocalDateTime antes);

    BigDecimal totalVentas(LocalDate desde, LocalDate hasta);

    BigDecimal costoVendido(LocalDate desde, LocalDate hasta);

    int cantidadVentas(LocalDate desde, LocalDate hasta);

    BigDecimal ventasContado(LocalDate fecha, boolean efectivo);

    List<GananciaProducto> gananciaPorProducto(LocalDate desde, LocalDate hasta);

    List<PendienteFactura> pendientesFacturar(LocalDate desde, LocalDate hasta);

    List<VentaVista> sinFacturaCliente(int idCliente);

    void asignarFactura(List<Long> idsVenta, int idFactura);

    BigDecimal totalAsociado(int idFactura);
}
