package pe.canomar.svc.dao.jdbc;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import pe.canomar.svc.dao.CompraDao;
import pe.canomar.svc.modelo.CompraVista;
import pe.canomar.svc.modelo.DeudaProveedor;
import pe.canomar.svc.modelo.ItemCompra;
import pe.canomar.svc.modelo.PagoProveedorVista;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public class JdbcCompraDao extends JdbcBase implements CompraDao {

    public JdbcCompraDao(JdbcTemplate jdbc) {
        super(jdbc);
    }

    @Override
    public int insertarCompra(int idProveedor, int idUsuario, LocalDate fecha, String tipoPago) {
        return (int) insertarConLlave("INSERT INTO compra (id_proveedor, id_usuario, fecha, tipo_pago, estado) VALUES (?, ?, ?, ?, 'VIGENTE')",
                idProveedor, idUsuario, fecha, tipoPago);
    }

    @Override
    public int insertarDetalle(int idCompra, ItemCompra item) {
        return (int) insertarConLlave("INSERT INTO detalle_compra (id_compra, id_producto, kilos, costo_total) VALUES (?, ?, ?, ?)",
                idCompra, item.idProducto(), item.kilos(), item.costoTotal());
    }

    @Override
    public List<CompraVista> listarPorFecha(LocalDate fecha) {
        return jdbc.query("SELECT c.id_compra, c.fecha, p.nombre, c.tipo_pago, COALESCE(SUM(d.kilos), 0) kilos, COALESCE(SUM(d.costo_total), 0) total "
                        + "FROM compra c JOIN proveedor p ON p.id_proveedor = c.id_proveedor LEFT JOIN detalle_compra d ON d.id_compra = c.id_compra "
                        + "WHERE c.fecha = ? AND c.estado = 'VIGENTE' GROUP BY c.id_compra, c.fecha, p.nombre, c.tipo_pago ORDER BY c.id_compra DESC",
                (rs, i) -> new CompraVista(rs.getInt(1), fecha(rs, "fecha"), rs.getString(3), rs.getString(4), rs.getBigDecimal(5), rs.getBigDecimal(6)), fecha);
    }

    @Override
    public List<DeudaProveedor> deudas() {
        String sql = "SELECT p.id_proveedor, p.nombre, p.tipo, "
                + "(SELECT COALESCE(SUM(d.costo_total), 0) FROM compra c JOIN detalle_compra d ON d.id_compra = c.id_compra "
                + "  WHERE c.id_proveedor = p.id_proveedor AND c.tipo_pago = 'CREDITO' AND c.estado = 'VIGENTE') compras, "
                + "(SELECT COALESCE(SUM(pp.monto), 0) FROM pago_proveedor pp WHERE pp.id_proveedor = p.id_proveedor) pagos, "
                + "(SELECT MIN(c.fecha) FROM compra c WHERE c.id_proveedor = p.id_proveedor AND c.tipo_pago = 'CREDITO' AND c.estado = 'VIGENTE') antigua "
                + "FROM proveedor p ORDER BY p.nombre";
        return jdbc.query(sql, (rs, i) -> new DeudaProveedor(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getBigDecimal(4), rs.getBigDecimal(5), fecha(rs, "antigua")));
    }

    @Override
    public BigDecimal totalCompras(LocalDate desde, LocalDate hasta) {
        return monto("SELECT COALESCE(SUM(d.costo_total), 0) FROM compra c JOIN detalle_compra d ON d.id_compra = c.id_compra "
                + "WHERE c.estado = 'VIGENTE' AND c.fecha BETWEEN ? AND ?", desde, hasta);
    }

    @Override
    public int insertarPago(int idProveedor, int idUsuario, LocalDate fecha, BigDecimal monto, String medio) {
        return (int) insertarConLlave("INSERT INTO pago_proveedor (id_proveedor, id_usuario, fecha, monto, medio) VALUES (?, ?, ?, ?, ?)",
                idProveedor, idUsuario, fecha, monto, medio);
    }

    @Override
    public List<PagoProveedorVista> pagosRecientes(int limite) {
        return jdbc.query("SELECT pp.id_pago, pp.fecha, p.nombre, pp.monto, pp.medio FROM pago_proveedor pp JOIN proveedor p ON p.id_proveedor = pp.id_proveedor "
                        + "ORDER BY pp.fecha DESC, pp.id_pago DESC LIMIT ?",
                (rs, i) -> new PagoProveedorVista(rs.getInt(1), fecha(rs, "fecha"), rs.getString(3), rs.getBigDecimal(4), rs.getString(5)), limite);
    }

    @Override
    public BigDecimal pagosDelDia(LocalDate fecha, boolean efectivo) {
        return monto("SELECT COALESCE(SUM(monto), 0) FROM pago_proveedor WHERE fecha = ? AND (medio = 'EFECTIVO') = ?", fecha, efectivo);
    }
}
