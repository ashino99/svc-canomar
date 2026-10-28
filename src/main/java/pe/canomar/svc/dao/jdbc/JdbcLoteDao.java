package pe.canomar.svc.dao.jdbc;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import pe.canomar.svc.comun.Calculos.LoteKg;
import pe.canomar.svc.comun.Dinero;
import pe.canomar.svc.dao.LoteDao;
import pe.canomar.svc.modelo.LoteVista;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class JdbcLoteDao extends JdbcBase implements LoteDao {

    public JdbcLoteDao(JdbcTemplate jdbc) {
        super(jdbc);
    }

    @Override
    public int insertar(int idProducto, Integer idDetalleCompra, LocalDate fechaIngreso, BigDecimal kilos, BigDecimal costoKg) {
        return (int) insertarConLlave("INSERT INTO lote_saldo (id_producto, id_detalle_compra, fecha_ingreso, kilos_disponibles, costo_kg) VALUES (?, ?, ?, ?, ?)",
                idProducto, idDetalleCompra, fechaIngreso, kilos, costoKg);
    }

    @Override
    public List<LoteKg> disponiblesFifo(int idProducto) {
        return jdbc.query("SELECT id_lote, kilos_disponibles, costo_kg FROM lote_saldo WHERE id_producto = ? AND kilos_disponibles > 0 "
                        + "ORDER BY fecha_ingreso, id_lote",
                (rs, i) -> new LoteKg(rs.getInt(1), rs.getBigDecimal(2), rs.getBigDecimal(3)), idProducto);
    }

    @Override
    public List<LoteVista> lotesConKilos() {
        return jdbc.query("SELECT l.id_lote, l.id_producto, p.nombre, l.fecha_ingreso, l.kilos_disponibles, l.costo_kg "
                        + "FROM lote_saldo l JOIN producto p ON p.id_producto = l.id_producto WHERE l.kilos_disponibles > 0 "
                        + "ORDER BY l.fecha_ingreso, p.nombre",
                (rs, i) -> new LoteVista(rs.getInt(1), rs.getInt(2), rs.getString(3), fecha(rs, "fecha_ingreso"), rs.getBigDecimal(5), rs.getBigDecimal(6)));
    }

    @Override
    public void descontar(int idLote, BigDecimal kilos) {
        jdbc.update("UPDATE lote_saldo SET kilos_disponibles = GREATEST(kilos_disponibles - ?, 0) WHERE id_lote = ?", kilos, idLote);
    }

    @Override
    public void sumar(int idLote, BigDecimal kilos) {
        jdbc.update("UPDATE lote_saldo SET kilos_disponibles = kilos_disponibles + ? WHERE id_lote = ?", kilos, idLote);
    }

    @Override
    public Optional<Integer> ultimoLote(int idProducto) {
        return jdbc.queryForList("SELECT id_lote FROM lote_saldo WHERE id_producto = ? ORDER BY fecha_ingreso DESC, id_lote DESC LIMIT 1",
                Integer.class, idProducto).stream().findFirst();
    }

    @Override
    public BigDecimal kilosDisponibles(int idProducto) {
        return monto("SELECT COALESCE(SUM(kilos_disponibles), 0) FROM lote_saldo WHERE id_producto = ?", idProducto);
    }

    @Override
    public Map<Integer, BigDecimal> kilosPorProducto() {
        Map<Integer, BigDecimal> m = new HashMap<>();
        jdbc.query("SELECT id_producto, SUM(kilos_disponibles) FROM lote_saldo GROUP BY id_producto",
                rs -> {
                    m.put(rs.getInt(1), Dinero.r2(rs.getBigDecimal(2)));
                });
        return m;
    }

    @Override
    public int insertarAjuste(int idLote, int idUsuario, LocalDateTime fecha, String tipo, BigDecimal kilos, BigDecimal valorCosto, String motivo) {
        return (int) insertarConLlave("INSERT INTO ajuste_saldo (id_lote, id_usuario, fecha, tipo, kilos, valor_costo, motivo) VALUES (?, ?, ?, ?, ?, ?, ?)",
                idLote, idUsuario, fecha, tipo, kilos, valorCosto, motivo);
    }

    @Override
    public BigDecimal perdidas(LocalDate desde, LocalDate hasta) {
        return monto("SELECT COALESCE(SUM(valor_costo), 0) FROM ajuste_saldo WHERE fecha >= ? AND fecha < ?", inicio(desde), finExclusivo(hasta));
    }
}
