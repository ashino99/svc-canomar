package pe.canomar.svc.dao.jdbc;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import pe.canomar.svc.dao.ClienteDao;
import pe.canomar.svc.modelo.Cliente;

import java.util.List;
import java.util.Optional;

@Repository
public class JdbcClienteDao extends JdbcBase implements ClienteDao {

    private static final String SELECT = "SELECT id_cliente, nombre_comercial, ruc, telefono, limite_credito, plazo_dias, estado FROM cliente ";
    private static final RowMapper<Cliente> MAPPER = (rs, i) -> new Cliente(rs.getInt("id_cliente"), rs.getString("nombre_comercial"),
            rs.getString("ruc"), rs.getString("telefono"), rs.getBigDecimal("limite_credito"), rs.getInt("plazo_dias"), rs.getString("estado"));

    public JdbcClienteDao(JdbcTemplate jdbc) {
        super(jdbc);
    }

    @Override
    public List<Cliente> listar() {
        return jdbc.query(SELECT + "ORDER BY estado, nombre_comercial", MAPPER);
    }

    @Override
    public List<Cliente> listarActivos() {
        return jdbc.query(SELECT + "WHERE estado = 'ACTIVO' ORDER BY nombre_comercial", MAPPER);
    }

    @Override
    public Optional<Cliente> buscar(int idCliente) {
        return jdbc.query(SELECT + "WHERE id_cliente = ?", MAPPER, idCliente).stream().findFirst();
    }

    @Override
    public boolean existeRuc(String ruc, Integer excluirId) {
        return entero("SELECT COUNT(*) FROM cliente WHERE ruc = ? AND id_cliente <> ?", ruc, excluirId == null ? 0 : excluirId) > 0;
    }

    @Override
    public int insertar(Cliente c) {
        return (int) insertarConLlave("INSERT INTO cliente (nombre_comercial, ruc, telefono, limite_credito, plazo_dias, estado) VALUES (?, ?, ?, ?, ?, ?)",
                c.nombreComercial(), c.ruc(), c.telefono(), c.limiteCredito(), c.plazoDias(), c.estado());
    }

    @Override
    public void actualizar(Cliente c) {
        jdbc.update("UPDATE cliente SET nombre_comercial = ?, ruc = ?, telefono = ?, limite_credito = ?, plazo_dias = ?, estado = ? WHERE id_cliente = ?",
                c.nombreComercial(), c.ruc(), c.telefono(), c.limiteCredito(), c.plazoDias(), c.estado(), c.id());
    }
}
