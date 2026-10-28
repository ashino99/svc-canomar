package pe.canomar.svc.dao.jdbc;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import pe.canomar.svc.comun.Fechas;
import pe.canomar.svc.dao.RespaldoDao;

import java.sql.ResultSetMetaData;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Genera un script SQL con los datos de todas las tablas, en orden de dependencias. */
@Repository
public class JdbcRespaldoDao extends JdbcBase implements RespaldoDao {

    private static final List<String> TABLAS = List.of("configuracion", "usuario", "producto", "cliente", "proveedor", "compra", "detalle_compra",
            "pago_proveedor", "precio_dia", "lote_saldo", "ajuste_saldo", "factura", "venta", "detalle_venta", "abono", "abono_venta",
            "compromiso_pago", "gasto", "cuadre", "auditoria");

    private static final DateTimeFormatter SQL_FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public JdbcRespaldoDao(JdbcTemplate jdbc) {
        super(jdbc);
    }

    @Override
    public String exportarSql() {
        StringBuilder sb = new StringBuilder();
        sb.append("-- Respaldo de svc_canomar generado el ").append(Fechas.ahora()).append("\n");
        sb.append("-- Restaurar: crear las tablas con schema.sql y luego ejecutar este archivo.\n");
        sb.append("SET FOREIGN_KEY_CHECKS = 0;\n\n");
        for (String tabla : TABLAS) {
            sb.append("-- ").append(tabla).append("\nDELETE FROM ").append(tabla).append(";\n");
            // Las tablas son una lista fija: no hay datos del usuario en el nombre de la tabla.
            jdbc.query("SELECT * FROM " + tabla, rs -> {
                ResultSetMetaData md = rs.getMetaData();
                int n = md.getColumnCount();
                sb.append("INSERT INTO ").append(tabla).append(" VALUES (");
                for (int c = 1; c <= n; c++) {
                    Object v = rs.getObject(c);
                    if (c > 1) {
                        sb.append(", ");
                    }
                    if (v == null) {
                        sb.append("NULL");
                    } else if (v instanceof Number || v instanceof Boolean) {
                        sb.append(v instanceof Boolean b ? (b ? "1" : "0") : v.toString());
                    } else if (v instanceof LocalDateTime ldt) {
                        sb.append('\'').append(ldt.format(SQL_FECHA_HORA)).append('\'');
                    } else {
                        sb.append('\'').append(v.toString().replace("\\", "\\\\").replace("'", "''")).append('\'');
                    }
                }
                sb.append(");\n");
            });
            sb.append('\n');
        }
        sb.append("SET FOREIGN_KEY_CHECKS = 1;\n");
        return sb.toString();
    }
}
