package pe.canomar.svc.dao.jdbc;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import pe.canomar.svc.dao.GastoDao;
import pe.canomar.svc.modelo.GastoVista;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public class JdbcGastoDao extends JdbcBase implements GastoDao {

    public JdbcGastoDao(JdbcTemplate jdbc) {
        super(jdbc);
    }

    @Override
    public int insertar(int idUsuario, LocalDate fecha, String concepto, String descripcion, BigDecimal monto, String medio) {
        return (int) insertarConLlave("INSERT INTO gasto (id_usuario, fecha, concepto, descripcion, monto, medio) VALUES (?, ?, ?, ?, ?, ?)",
                idUsuario, fecha, concepto, descripcion, monto, medio);
    }

    @Override
    public List<GastoVista> listarPorFecha(LocalDate fecha) {
        return jdbc.query("SELECT g.id_gasto, g.fecha, g.concepto, g.descripcion, g.monto, g.medio, u.usuario FROM gasto g "
                        + "JOIN usuario u ON u.id_usuario = g.id_usuario WHERE g.fecha = ? ORDER BY g.id_gasto DESC",
                (rs, i) -> new GastoVista(rs.getInt(1), fecha(rs, "fecha"), rs.getString(3), rs.getString(4), rs.getBigDecimal(5), rs.getString(6), rs.getString(7)), fecha);
    }

    @Override
    public BigDecimal total(LocalDate desde, LocalDate hasta) {
        return monto("SELECT COALESCE(SUM(monto), 0) FROM gasto WHERE fecha BETWEEN ? AND ?", desde, hasta);
    }

    @Override
    public BigDecimal totalDelDia(LocalDate fecha, boolean efectivo) {
        return monto("SELECT COALESCE(SUM(monto), 0) FROM gasto WHERE fecha = ? AND (medio = 'EFECTIVO') = ?", fecha, efectivo);
    }
}
