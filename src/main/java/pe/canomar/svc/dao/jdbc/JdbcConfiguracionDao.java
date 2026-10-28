package pe.canomar.svc.dao.jdbc;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import pe.canomar.svc.dao.ConfiguracionDao;
import pe.canomar.svc.modelo.Configuracion;

import java.math.BigDecimal;

@Repository
public class JdbcConfiguracionDao extends JdbcBase implements ConfiguracionDao {

    public JdbcConfiguracionDao(JdbcTemplate jdbc) {
        super(jdbc);
    }

    @Override
    public Configuracion obtener() {
        return jdbc.query("SELECT dias_alerta_saldo, tolerancia_consumo, tolerancia_cuadre FROM configuracion ORDER BY id_configuracion LIMIT 1",
                        (rs, i) -> new Configuracion(rs.getInt(1), rs.getBigDecimal(2), rs.getBigDecimal(3)))
                .stream().findFirst()
                .orElse(new Configuracion(3, new BigDecimal("5.00"), new BigDecimal("5.00")));
    }
}
