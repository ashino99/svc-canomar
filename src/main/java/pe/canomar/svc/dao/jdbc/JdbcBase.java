package pe.canomar.svc.dao.jdbc;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import pe.canomar.svc.comun.Dinero;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Utilidades comunes de los DAO JDBC. Todas las consultas usan parámetros (?) para evitar inyección SQL. */
public abstract class JdbcBase {

    /** Total de una venta: suma de kilos × precio de sus líneas (no se guarda, se calcula). */
    static final String TOTAL_VENTA = "(SELECT COALESCE(SUM(ROUND(d.kilos * d.precio_kg, 2)), 0) FROM detalle_venta d WHERE d.id_venta = v.id_venta)";
    /** Parte pagada de una venta al crédito con abonos vigentes. */
    static final String PAGADO_VENTA = "(SELECT COALESCE(SUM(av.monto_aplicado), 0) FROM abono_venta av JOIN abono a ON a.id_abono = av.id_abono "
            + "WHERE av.id_venta = v.id_venta AND a.estado = 'VIGENTE')";

    protected final JdbcTemplate jdbc;

    protected JdbcBase(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Ejecuta un INSERT y devuelve la llave generada. */
    protected long insertarConLlave(String sql, Object... params) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            return ps;
        }, kh);
        Number key = kh.getKey();
        if (key == null) {
            throw new IllegalStateException("No se obtuvo la llave generada");
        }
        return key.longValue();
    }

    protected BigDecimal monto(String sql, Object... params) {
        BigDecimal v = jdbc.queryForObject(sql, BigDecimal.class, params);
        return Dinero.r2(v);
    }

    protected int entero(String sql, Object... params) {
        Integer v = jdbc.queryForObject(sql, Integer.class, params);
        return v == null ? 0 : v;
    }

    protected static LocalDate fecha(ResultSet rs, String col) throws SQLException {
        return rs.getObject(col, LocalDate.class);
    }

    protected static LocalDateTime fechaHora(ResultSet rs, String col) throws SQLException {
        return rs.getObject(col, LocalDateTime.class);
    }

    protected static Integer enteroNulo(ResultSet rs, String col) throws SQLException {
        int v = rs.getInt(col);
        return rs.wasNull() ? null : v;
    }

    protected static LocalDateTime inicio(LocalDate d) {
        return d.atStartOfDay();
    }

    protected static LocalDateTime finExclusivo(LocalDate d) {
        return d.plusDays(1).atStartOfDay();
    }
}
