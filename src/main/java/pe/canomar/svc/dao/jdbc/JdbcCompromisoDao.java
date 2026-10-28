package pe.canomar.svc.dao.jdbc;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import pe.canomar.svc.dao.CompromisoDao;
import pe.canomar.svc.modelo.CompromisoVista;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public class JdbcCompromisoDao extends JdbcBase implements CompromisoDao {

    public JdbcCompromisoDao(JdbcTemplate jdbc) {
        super(jdbc);
    }

    @Override
    public int insertar(int idCliente, LocalDate fecha, BigDecimal monto) {
        return (int) insertarConLlave("INSERT INTO compromiso_pago (id_cliente, fecha_compromiso, monto, estado) VALUES (?, ?, ?, 'PENDIENTE')",
                idCliente, fecha, monto);
    }

    @Override
    public List<CompromisoVista> listarVigentes() {
        return jdbc.query("SELECT cp.id_compromiso, cp.id_cliente, c.nombre_comercial, cp.fecha_compromiso, cp.monto, cp.estado "
                        + "FROM compromiso_pago cp JOIN cliente c ON c.id_cliente = cp.id_cliente "
                        + "WHERE cp.estado <> 'CUMPLIDO' ORDER BY cp.fecha_compromiso",
                (rs, i) -> new CompromisoVista(rs.getInt(1), rs.getInt(2), rs.getString(3), fecha(rs, "fecha_compromiso"), rs.getBigDecimal(5), rs.getString(6)));
    }

    @Override
    public int marcarIncumplidos(LocalDate hoy) {
        return jdbc.update("UPDATE compromiso_pago SET estado = 'INCUMPLIDO' WHERE estado = 'PENDIENTE' AND fecha_compromiso < ?", hoy);
    }

    @Override
    public void marcarCumplidos(int idCliente) {
        jdbc.update("UPDATE compromiso_pago SET estado = 'CUMPLIDO' WHERE id_cliente = ? AND estado <> 'CUMPLIDO'", idCliente);
    }
}
