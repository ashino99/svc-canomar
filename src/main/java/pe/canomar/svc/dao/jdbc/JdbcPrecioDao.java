package pe.canomar.svc.dao.jdbc;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import pe.canomar.svc.dao.PrecioDao;
import pe.canomar.svc.modelo.PrecioVista;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcPrecioDao extends JdbcBase implements PrecioDao {

    public JdbcPrecioDao(JdbcTemplate jdbc) {
        super(jdbc);
    }

    @Override
    public List<PrecioVista> preciosDe(LocalDate fecha) {
        String sql = "SELECT p.id_producto, p.nombre, p.nombre_corto, "
                + "(SELECT pd.costo_promedio_kg FROM precio_dia pd WHERE pd.id_producto = p.id_producto AND pd.fecha <= ? ORDER BY pd.fecha DESC LIMIT 1) costo, "
                + "(SELECT pd.precio_kg FROM precio_dia pd WHERE pd.id_producto = p.id_producto AND pd.fecha = ?) hoy, "
                + "(SELECT pd.precio_kg FROM precio_dia pd WHERE pd.id_producto = p.id_producto AND pd.fecha < ? ORDER BY pd.fecha DESC LIMIT 1) anterior, "
                + "(SELECT COALESCE(SUM(l.kilos_disponibles), 0) FROM lote_saldo l WHERE l.id_producto = p.id_producto) kilos "
                + "FROM producto p WHERE p.estado = 'ACTIVO' ORDER BY p.nombre";
        return jdbc.query(sql, (rs, i) -> new PrecioVista(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getBigDecimal(4),
                rs.getBigDecimal(5), rs.getBigDecimal(6), rs.getBigDecimal(7)), fecha, fecha, fecha);
    }

    @Override
    public Optional<BigDecimal> precioDe(int idProducto, LocalDate fecha) {
        return jdbc.queryForList("SELECT precio_kg FROM precio_dia WHERE id_producto = ? AND fecha = ?", BigDecimal.class, idProducto, fecha)
                .stream().findFirst();
    }

    @Override
    public Optional<BigDecimal> ultimoPrecio(int idProducto, LocalDate antesDe) {
        return jdbc.queryForList("SELECT precio_kg FROM precio_dia WHERE id_producto = ? AND fecha < ? ORDER BY fecha DESC LIMIT 1",
                BigDecimal.class, idProducto, antesDe).stream().findFirst();
    }

    @Override
    public Optional<BigDecimal> ultimoCosto(int idProducto, LocalDate hasta) {
        return jdbc.queryForList("SELECT costo_promedio_kg FROM precio_dia WHERE id_producto = ? AND fecha <= ? ORDER BY fecha DESC LIMIT 1",
                BigDecimal.class, idProducto, hasta).stream().findFirst();
    }

    @Override
    public void guardarPrecio(int idProducto, LocalDate fecha, BigDecimal precioKg, BigDecimal costoKg) {
        int n = jdbc.update("UPDATE precio_dia SET precio_kg = ? WHERE id_producto = ? AND fecha = ?", precioKg, idProducto, fecha);
        if (n == 0) {
            jdbc.update("INSERT INTO precio_dia (id_producto, fecha, precio_kg, costo_promedio_kg) VALUES (?, ?, ?, ?)", idProducto, fecha, precioKg, costoKg);
        }
    }

    @Override
    public void guardarCosto(int idProducto, LocalDate fecha, BigDecimal costoKg, BigDecimal precioPorDefecto) {
        int n = jdbc.update("UPDATE precio_dia SET costo_promedio_kg = ? WHERE id_producto = ? AND fecha = ?", costoKg, idProducto, fecha);
        if (n == 0) {
            jdbc.update("INSERT INTO precio_dia (id_producto, fecha, precio_kg, costo_promedio_kg) VALUES (?, ?, ?, ?)", idProducto, fecha, precioPorDefecto, costoKg);
        }
    }
}
