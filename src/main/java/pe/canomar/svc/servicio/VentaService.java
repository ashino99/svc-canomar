package pe.canomar.svc.servicio;

import pe.canomar.svc.modelo.DetalleVentaVista;
import pe.canomar.svc.modelo.NuevaVenta;
import pe.canomar.svc.modelo.ResultadoSync;
import pe.canomar.svc.modelo.VentaVista;

import java.time.LocalDate;
import java.util.List;

/** Ventas por kilo al contado, al crédito y de remate (RF17–RF26). */
public interface VentaService {
    /** Registra una venta. Lanza ConfirmacionException si requiere confirmación y no está confirmada. */
    VentaVista registrar(NuevaVenta venta, int idActor, boolean esAdministrador, boolean confirmado);

    /** RF25: registra las ventas guardadas en el celular sin conexión; ignora las que ya llegaron. */
    List<ResultadoSync> sincronizar(List<NuevaVenta> ventas, int idActor);

    void anular(long idVenta, String motivo, int idActor);

    List<VentaVista> ventasDelDia(LocalDate fecha);

    VentaVista buscar(long idVenta);

    List<DetalleVentaVista> detalle(long idVenta);
}
