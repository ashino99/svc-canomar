package pe.canomar.svc.dao.jdbc;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import pe.canomar.svc.dao.VentaDao;
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

@Repository
public class JdbcVentaDao extends JdbcBase implements VentaDao {

    private static final String SELECT = "SELECT v.id_venta, v.uuid_dispositivo, v.fecha_hora, v.id_cliente, c.nombre_comercial, v.tipo_pago, v.medio_pago, "
            + "v.es_remate, v.estado, v.motivo_anulacion, " + TOTAL_VENTA + " total, u.usuario, v.id_factura, v.fecha_sincronizacion "
            + "FROM venta v JOIN usuario u ON u.id_usuario = v.id_usuario LEFT JOIN cliente c ON c.id_cliente = v.id_cliente ";

    private static final RowMapper<VentaVista> MAPPER = (rs, i) -> new VentaVista(rs.getLong("id_venta"), rs.getString("uuid_dispositivo"),
            fechaHora(rs, "fecha_hora"), enteroNulo(rs, "id_cliente"), rs.getString("nombre_comercial") == null ? "Público" : rs.getString("nombre_comercial"),
            rs.getString("tipo_pago"), rs.getString("medio_pago"), rs.getBoolean("es_remate"), rs.getString("estado"), rs.getString("motivo_anulacion"),
            rs.getBigDecimal("total"), rs.getString("usuario"), enteroNulo(rs, "id_factura"), rs.getObject("fecha_sincronizacion") != null);

    public JdbcVentaDao(JdbcTemplate jdbc) {
        super(jdbc);
    }

    @Override
    public long insertarVenta(NuevaVenta v, int idUsuario, LocalDateTime fechaSincronizacion) {
        return insertarConLlave("INSERT INTO venta (uuid_dispositivo, id_usuario, id_cliente, fecha_hora, fecha_sincronizacion, tipo_pago, medio_pago, es_remate, estado) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'VIGENTE')",
                v.uuid(), idUsuario, v.idCliente(), v.fechaHora(), fechaSincronizacion, v.tipoPago(), v.medioPago(), v.esRemate());
    }

    @Override
    public void insertarDetalle(long idVenta, int idProducto, BigDecimal kilos, BigDecimal precioKg, BigDecimal costoKg) {
        jdbc.update("INSERT INTO detalle_venta (id_venta, id_producto, kilos, precio_kg, costo_kg) VALUES (?, ?, ?, ?, ?)",
                idVenta, idProducto, kilos, precioKg, costoKg);
    }

    @Override
    public boolean existeUuid(String uuid) {
        return entero("SELECT COUNT(*) FROM venta WHERE uuid_dispositivo = ?", uuid) > 0;
    }

    @Override
    public Optional<VentaVista> buscar(long idVenta) {
        return jdbc.query(SELECT + "WHERE v.id_venta = ?", MAPPER, idVenta).stream().findFirst();
    }

    @Override
    public List<DetalleVentaVista> detalle(long idVenta) {
        return jdbc.query("SELECT d.id_producto, p.nombre, d.kilos, d.precio_kg, d.costo_kg FROM detalle_venta d JOIN producto p ON p.id_producto = d.id_producto "
                        + "WHERE d.id_venta = ? ORDER BY d.id_detalle_venta",
                (rs, i) -> new DetalleVentaVista(rs.getInt(1), rs.getString(2), rs.getBigDecimal(3), rs.getBigDecimal(4), rs.getBigDecimal(5)), idVenta);
    }

    @Override
    public List<VentaVista> listarEntre(LocalDate desde, LocalDate hasta) {
        return jdbc.query(SELECT + "WHERE v.fecha_hora >= ? AND v.fecha_hora < ? ORDER BY v.fecha_hora DESC, v.id_venta DESC", MAPPER,
                inicio(desde), finExclusivo(hasta));
    }

    @Override
    public void anular(long idVenta, String motivo) {
        jdbc.update("UPDATE venta SET estado = 'ANULADA', motivo_anulacion = ? WHERE id_venta = ?", motivo, idVenta);
    }

    @Override
    public List<PendienteVenta> pendientesCliente(int idCliente) {
        return jdbc.query("SELECT v.id_venta, v.fecha_hora, " + TOTAL_VENTA + " total, " + PAGADO_VENTA + " pagado FROM venta v "
                        + "WHERE v.id_cliente = ? AND v.tipo_pago = 'CREDITO' AND v.estado = 'VIGENTE' ORDER BY v.fecha_hora, v.id_venta",
                (rs, i) -> new PendienteVenta(rs.getLong(1), fechaHora(rs, "fecha_hora"), rs.getBigDecimal("total"), rs.getBigDecimal("pagado")), idCliente);
    }

    @Override
    public List<SaldoCliente> saldosClientes(LocalDate hoy) {
        String pend = "SELECT v.id_cliente, v.fecha_hora, " + TOTAL_VENTA + " - " + PAGADO_VENTA + " pendiente FROM venta v "
                + "WHERE v.tipo_pago = 'CREDITO' AND v.estado = 'VIGENTE'";
        String sql = "SELECT c.id_cliente, c.nombre_comercial, c.ruc, c.limite_credito, c.plazo_dias, "
                + "COALESCE(SUM(p.pendiente), 0) saldo, "
                + "COALESCE(SUM(CASE WHEN DATEDIFF(?, DATE(p.fecha_hora)) > c.plazo_dias THEN p.pendiente ELSE 0 END), 0) vencido, "
                + "MIN(CASE WHEN p.pendiente > 0 THEN DATE(p.fecha_hora) END) antigua "
                + "FROM cliente c LEFT JOIN (" + pend + ") p ON p.id_cliente = c.id_cliente "
                + "WHERE c.estado = 'ACTIVO' GROUP BY c.id_cliente, c.nombre_comercial, c.ruc, c.limite_credito, c.plazo_dias "
                + "ORDER BY vencido DESC, saldo DESC, c.nombre_comercial";
        return jdbc.query(sql, (rs, i) -> new SaldoCliente(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getBigDecimal(4), rs.getInt(5),
                rs.getBigDecimal("saldo"), rs.getBigDecimal("vencido"), fecha(rs, "antigua")), hoy);
    }

    @Override
    public List<VentaVista> ventasCreditoCliente(int idCliente, LocalDateTime desde, LocalDateTime hasta) {
        return jdbc.query(SELECT + "WHERE v.id_cliente = ? AND v.tipo_pago = 'CREDITO' AND v.estado = 'VIGENTE' AND v.fecha_hora >= ? AND v.fecha_hora < ? "
                + "ORDER BY v.fecha_hora", MAPPER, idCliente, desde, hasta);
    }

    @Override
    public BigDecimal ventasCreditoAntesDe(int idCliente, LocalDateTime antes) {
        return monto("SELECT COALESCE(SUM(ROUND(d.kilos * d.precio_kg, 2)), 0) FROM venta v JOIN detalle_venta d ON d.id_venta = v.id_venta "
                + "WHERE v.id_cliente = ? AND v.tipo_pago = 'CREDITO' AND v.estado = 'VIGENTE' AND v.fecha_hora < ?", idCliente, antes);
    }

    @Override
    public BigDecimal totalVentas(LocalDate desde, LocalDate hasta) {
        return monto("SELECT COALESCE(SUM(ROUND(d.kilos * d.precio_kg, 2)), 0) FROM venta v JOIN detalle_venta d ON d.id_venta = v.id_venta "
                + "WHERE v.estado = 'VIGENTE' AND v.fecha_hora >= ? AND v.fecha_hora < ?", inicio(desde), finExclusivo(hasta));
    }

    @Override
    public BigDecimal costoVendido(LocalDate desde, LocalDate hasta) {
        return monto("SELECT COALESCE(SUM(ROUND(d.kilos * d.costo_kg, 2)), 0) FROM venta v JOIN detalle_venta d ON d.id_venta = v.id_venta "
                + "WHERE v.estado = 'VIGENTE' AND v.fecha_hora >= ? AND v.fecha_hora < ?", inicio(desde), finExclusivo(hasta));
    }

    @Override
    public int cantidadVentas(LocalDate desde, LocalDate hasta) {
        return entero("SELECT COUNT(*) FROM venta WHERE estado = 'VIGENTE' AND fecha_hora >= ? AND fecha_hora < ?", inicio(desde), finExclusivo(hasta));
    }

    @Override
    public BigDecimal ventasContado(LocalDate fecha, boolean efectivo) {
        return monto("SELECT COALESCE(SUM(ROUND(d.kilos * d.precio_kg, 2)), 0) FROM venta v JOIN detalle_venta d ON d.id_venta = v.id_venta "
                        + "WHERE v.estado = 'VIGENTE' AND v.tipo_pago = 'CONTADO' AND (COALESCE(v.medio_pago, 'EFECTIVO') = 'EFECTIVO') = ? "
                        + "AND v.fecha_hora >= ? AND v.fecha_hora < ?",
                efectivo, inicio(fecha), finExclusivo(fecha));
    }

    @Override
    public List<GananciaProducto> gananciaPorProducto(LocalDate desde, LocalDate hasta) {
        return jdbc.query("SELECT p.nombre, SUM(d.kilos) kilos, SUM(ROUND(d.kilos * d.precio_kg, 2)) ventas, SUM(ROUND(d.kilos * d.costo_kg, 2)) costo "
                        + "FROM venta v JOIN detalle_venta d ON d.id_venta = v.id_venta JOIN producto p ON p.id_producto = d.id_producto "
                        + "WHERE v.estado = 'VIGENTE' AND v.fecha_hora >= ? AND v.fecha_hora < ? GROUP BY p.nombre "
                        + "ORDER BY SUM(ROUND(d.kilos * d.precio_kg, 2)) - SUM(ROUND(d.kilos * d.costo_kg, 2)) DESC",
                (rs, i) -> new GananciaProducto(rs.getString(1), rs.getBigDecimal(2), rs.getBigDecimal(3), rs.getBigDecimal(4)),
                inicio(desde), finExclusivo(hasta));
    }

    @Override
    public List<PendienteFactura> pendientesFacturar(LocalDate desde, LocalDate hasta) {
        return jdbc.query("SELECT c.id_cliente, c.nombre_comercial, c.ruc, COUNT(DISTINCT v.id_venta) ventas, SUM(ROUND(d.kilos * d.precio_kg, 2)) total "
                        + "FROM venta v JOIN cliente c ON c.id_cliente = v.id_cliente JOIN detalle_venta d ON d.id_venta = v.id_venta "
                        + "WHERE v.estado = 'VIGENTE' AND v.id_factura IS NULL AND c.ruc IS NOT NULL AND v.fecha_hora >= ? AND v.fecha_hora < ? "
                        + "GROUP BY c.id_cliente, c.nombre_comercial, c.ruc ORDER BY total DESC",
                (rs, i) -> new PendienteFactura(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getInt(4), rs.getBigDecimal(5)),
                inicio(desde), finExclusivo(hasta));
    }

    @Override
    public List<VentaVista> sinFacturaCliente(int idCliente) {
        return jdbc.query(SELECT + "WHERE v.id_cliente = ? AND v.estado = 'VIGENTE' AND v.id_factura IS NULL ORDER BY v.fecha_hora", MAPPER, idCliente);
    }

    @Override
    public void asignarFactura(List<Long> idsVenta, int idFactura) {
        for (Long id : idsVenta) {
            jdbc.update("UPDATE venta SET id_factura = ? WHERE id_venta = ? AND id_factura IS NULL", idFactura, id);
        }
    }

    @Override
    public BigDecimal totalAsociado(int idFactura) {
        return monto("SELECT COALESCE(SUM(ROUND(d.kilos * d.precio_kg, 2)), 0) FROM venta v JOIN detalle_venta d ON d.id_venta = v.id_venta "
                + "WHERE v.id_factura = ? AND v.estado = 'VIGENTE'", idFactura);
    }
}
