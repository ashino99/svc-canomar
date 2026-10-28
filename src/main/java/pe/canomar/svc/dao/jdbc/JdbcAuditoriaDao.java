package pe.canomar.svc.dao.jdbc;

import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import pe.canomar.svc.comun.Fechas;
import pe.canomar.svc.dao.AuditoriaDao;
import pe.canomar.svc.modelo.AuditoriaVista;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class JdbcAuditoriaDao extends JdbcBase implements AuditoriaDao {

    public JdbcAuditoriaDao(JdbcTemplate jdbc) {
        super(jdbc);
    }

    @Override
    public void registrar(Integer idUsuario, String operacion, String detalle) {
        jdbc.update("INSERT INTO auditoria (id_usuario, fecha_hora, operacion, detalle) VALUES (?, ?, ?, ?)",
                idUsuario, Fechas.ahora(), StringUtils.left(operacion, 40), StringUtils.abbreviate(detalle, 255));
    }

    @Override
    public List<AuditoriaVista> buscar(Integer idUsuario, String operacion, LocalDate desde, LocalDate hasta) {
        StringBuilder sql = new StringBuilder("SELECT a.id_auditoria, a.fecha_hora, COALESCE(u.usuario, '—') usuario, a.operacion, a.detalle "
                + "FROM auditoria a LEFT JOIN usuario u ON u.id_usuario = a.id_usuario WHERE a.fecha_hora >= ? AND a.fecha_hora < ? ");
        List<Object> params = new ArrayList<>();
        params.add(inicio(desde));
        params.add(finExclusivo(hasta));
        if (idUsuario != null) {
            sql.append("AND a.id_usuario = ? ");
            params.add(idUsuario);
        }
        if (StringUtils.isNotBlank(operacion)) {
            sql.append("AND a.operacion = ? ");
            params.add(operacion);
        }
        sql.append("ORDER BY a.fecha_hora DESC, a.id_auditoria DESC LIMIT 300");
        return jdbc.query(sql.toString(), (rs, i) -> new AuditoriaVista(rs.getLong(1), fechaHora(rs, "fecha_hora"), rs.getString(3),
                rs.getString(4), rs.getString(5)), params.toArray());
    }

    @Override
    public List<String> operaciones() {
        return jdbc.queryForList("SELECT DISTINCT operacion FROM auditoria ORDER BY operacion", String.class);
    }
}
