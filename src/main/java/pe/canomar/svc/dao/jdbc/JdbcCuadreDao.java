package pe.canomar.svc.dao.jdbc;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import pe.canomar.svc.dao.CuadreDao;
import pe.canomar.svc.modelo.CuadreVista;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcCuadreDao extends JdbcBase implements CuadreDao {

    private static final String SELECT = "SELECT c.id_cuadre, c.fecha, c.efectivo_esperado, c.efectivo_contado, c.digital_esperado, c.digital_revisado, "
            + "c.motivo_diferencia, c.ganancia, u.usuario FROM cuadre c JOIN usuario u ON u.id_usuario = c.id_usuario ";
    private static final RowMapper<CuadreVista> MAPPER = (rs, i) -> new CuadreVista(rs.getInt(1), fecha(rs, "fecha"), rs.getBigDecimal(3),
            rs.getBigDecimal(4), rs.getBigDecimal(5), rs.getBigDecimal(6), rs.getString(7), rs.getBigDecimal(8), rs.getString(9));

    public JdbcCuadreDao(JdbcTemplate jdbc) {
        super(jdbc);
    }

    @Override
    public int insertar(int idUsuario, LocalDate fecha, BigDecimal efectivoEsperado, BigDecimal efectivoContado,
                        BigDecimal digitalEsperado, BigDecimal digitalRevisado, String motivo, BigDecimal ganancia) {
        return (int) insertarConLlave("INSERT INTO cuadre (id_usuario, fecha, efectivo_esperado, efectivo_contado, digital_esperado, digital_revisado, "
                        + "motivo_diferencia, ganancia) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                idUsuario, fecha, efectivoEsperado, efectivoContado, digitalEsperado, digitalRevisado, motivo, ganancia);
    }

    @Override
    public Optional<CuadreVista> buscar(LocalDate fecha) {
        return jdbc.query(SELECT + "WHERE c.fecha = ?", MAPPER, fecha).stream().findFirst();
    }

    @Override
    public List<CuadreVista> listarEntre(LocalDate desde, LocalDate hasta) {
        return jdbc.query(SELECT + "WHERE c.fecha BETWEEN ? AND ? ORDER BY c.fecha DESC", MAPPER, desde, hasta);
    }

    @Override
    public int contarEntre(LocalDate desde, LocalDate hasta) {
        return entero("SELECT COUNT(*) FROM cuadre WHERE fecha BETWEEN ? AND ?", desde, hasta);
    }
}
