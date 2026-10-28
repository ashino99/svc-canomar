package pe.canomar.svc.dao;

import pe.canomar.svc.modelo.CompraVista;
import pe.canomar.svc.modelo.DeudaProveedor;
import pe.canomar.svc.modelo.ItemCompra;
import pe.canomar.svc.modelo.PagoProveedorVista;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Acceso a compra, detalle_compra y pago_proveedor. */
public interface CompraDao {
    int insertarCompra(int idProveedor, int idUsuario, LocalDate fecha, String tipoPago);

    int insertarDetalle(int idCompra, ItemCompra item);

    List<CompraVista> listarPorFecha(LocalDate fecha);

    List<DeudaProveedor> deudas();

    BigDecimal totalCompras(LocalDate desde, LocalDate hasta);

    int insertarPago(int idProveedor, int idUsuario, LocalDate fecha, BigDecimal monto, String medio);

    List<PagoProveedorVista> pagosRecientes(int limite);

    BigDecimal pagosDelDia(LocalDate fecha, boolean efectivo);
}
