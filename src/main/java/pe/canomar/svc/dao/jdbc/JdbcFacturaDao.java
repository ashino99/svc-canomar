package pe.canomar.svc.dao.jdbc;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import pe.canomar.svc.dao.FacturaDao;
import pe.canomar.svc.modelo.FacturaVista;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcFacturaDao extends JdbcBase implements FacturaDao {

    private static final String SELECT = "SELECT f.id_factura, f.id_cliente, c.nombre_comercial, f.serie, f.numero, f.fecha_emision, f.importe, f.sujeta_detraccion, "
            + "(SELECT COALESCE(SUM(ROUND(d.kilos * d.precio_kg, 2)), 0) FROM venta v JOIN detalle_venta d ON d.id_venta = v.id_venta "
            + " WHERE v.id_factura = f.id_factura AND v.estado = 'VIGENTE') asociado "
            + "FROM factura f JOIN cliente c ON c.id_cliente = f.id_cliente ";
    private static final RowMapper<FacturaVista> MAPPER = (rs, i) -> new FacturaVista(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getString(4),
            rs.getInt(5), fecha(rs, "fecha_emision"), rs.getBigDecimal(7), rs.getBoolean(8), rs.getBigDecimal("asociado"));

    public JdbcFacturaDao(JdbcTemplate jdbc) {
        super(jdbc);
    }

    @Override
    public int insertar(int idCliente, String serie, int numero, LocalDate fechaEmision, BigDecimal importe, boolean detraccion) {
        return (int) insertarConLlave("INSERT INTO factura (id_cliente, serie, numero, fecha_emision, importe, sujeta_detraccion) VALUES (?, ?, ?, ?, ?, ?)",
                idCliente, serie, numero, fechaEmision, importe, detraccion);
    }

    @Override
    public boolean existe(String serie, int numero) {
        return entero("SELECT COUNT(*) FROM factura WHERE serie = ? AND numero = ?", serie, numero) > 0;
    }

    @Override
    public List<FacturaVista> listar(int limite) {
        return jdbc.query(SELECT + "ORDER BY f.fecha_emision DESC, f.id_factura DESC LIMIT ?", MAPPER, limite);
    }

    @Override
    public Optional<FacturaVista> buscar(int idFactura) {
        return jdbc.query(SELECT + "WHERE f.id_factura = ?", MAPPER, idFactura).stream().findFirst();
    }

    @Override
    public BigDecimal totalEmitido(LocalDate desde, LocalDate hasta) {
        return monto("SELECT COALESCE(SUM(importe), 0) FROM factura WHERE fecha_emision BETWEEN ? AND ?", desde, hasta);
    }
}
