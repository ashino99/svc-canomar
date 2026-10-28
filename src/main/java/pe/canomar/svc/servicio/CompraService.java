package pe.canomar.svc.servicio;

import pe.canomar.svc.modelo.CompraVista;
import pe.canomar.svc.modelo.DeudaProveedor;
import pe.canomar.svc.modelo.ItemCompra;
import pe.canomar.svc.modelo.PagoProveedorVista;
import pe.canomar.svc.modelo.Proveedor;

import java.math.BigDecimal;
import java.util.List;

/** Compras de la madrugada, proveedores y pagos (RF10–RF14). */
public interface CompraService {
    List<Proveedor> proveedores();

    int registrarProveedor(String nombre, String ruc, String tipo, String telefono, int idActor);

    /** Registra la compra, crea los lotes de saldo y recalcula el costo promedio (RF11, RF12). */
    int registrarCompra(int idProveedor, String tipoPago, List<ItemCompra> items, int idActor);

    List<CompraVista> comprasDeHoy();

    List<DeudaProveedor> deudas();

    BigDecimal registrarPago(int idProveedor, BigDecimal monto, String medio, int idActor);

    List<PagoProveedorVista> pagosRecientes();
}
