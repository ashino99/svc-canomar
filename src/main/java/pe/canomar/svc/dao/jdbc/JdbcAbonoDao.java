package pe.canomar.svc.dao.jdbc;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import pe.canomar.svc.dao.AbonoDao;
import pe.canomar.svc.modelo.AbonoVista;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class JdbcAbonoDao extends JdbcBase implements AbonoDao {

    private static final String SELECT = "SELECT a.id_abono, a.fecha, c.nombre_comercial, a.monto, a.medio, a.estado FROM abono a JOIN cliente c ON c.id_cliente = a.id_cliente ";
    private static final RowMapper<AbonoVista> MAPPER = (rs, i) -> new AbonoVista(rs.getInt(1), fechaHora(rs, "fecha"), rs.getString(3),
            rs.getBigDecimal(4), rs.getString(5), rs.getString(6));

    public JdbcAbonoDao(JdbcTemplate jdbc) {
        super(jdbc);
    }

    @Override
    public int insertar(int idCliente, int idUsuario, LocalDateTime fecha, BigDecimal monto, String medio) {
        return (int) insertarConLlave("INSERT INTO abono (id_cliente, id_usuario, fecha, monto, medio, estado) VALUES (?, ?, ?, ?, ?, 'VIGENTE')",
                idCliente, idUsuario, fecha, monto, medio);
    }

    @Override
    public void aplicar(int idAbono, long idVenta, BigDecimal monto) {
        jdbc.update("INSERT INTO abono_venta (id_abono, id_venta, monto_aplicado) VALUES (?, ?, ?)", idAbono, idVenta, monto);
    }

    @Override
    public List<AbonoVista> recientes(int limite) {
        return jdbc.query(SELECT + "ORDER BY a.fecha DESC LIMIT ?", MAPPER, limite);
    }

    @Override
    public List<AbonoVista> abonosCliente(int idCliente, LocalDateTime desde, LocalDateTime hasta) {
        return jdbc.query(SELECT + "WHERE a.id_cliente = ? AND a.estado = 'VIGENTE' AND a.fecha >= ? AND a.fecha < ? ORDER BY a.fecha", MAPPER,
                idCliente, desde, hasta);
    }

    @Override
    public BigDecimal abonosAntesDe(int idCliente, LocalDateTime antes) {
        return monto("SELECT COALESCE(SUM(monto), 0) FROM abono WHERE id_cliente = ? AND estado = 'VIGENTE' AND fecha < ?", idCliente, antes);
    }

    @Override
    public BigDecimal abonosDelDia(LocalDate fecha, boolean efectivo) {
        return monto("SELECT COALESCE(SUM(monto), 0) FROM abono WHERE estado = 'VIGENTE' AND fecha >= ? AND fecha < ? AND (medio = 'EFECTIVO') = ?",
                inicio(fecha), finExclusivo(fecha), efectivo);
    }
}
