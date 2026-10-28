package pe.canomar.svc.dao.jdbc;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import pe.canomar.svc.dao.ProveedorDao;
import pe.canomar.svc.modelo.Proveedor;

import java.util.List;
import java.util.Optional;

@Repository
public class JdbcProveedorDao extends JdbcBase implements ProveedorDao {

    private static final String SELECT = "SELECT id_proveedor, nombre, ruc, tipo, telefono FROM proveedor ";
    private static final RowMapper<Proveedor> MAPPER = (rs, i) -> new Proveedor(rs.getInt("id_proveedor"), rs.getString("nombre"),
            rs.getString("ruc"), rs.getString("tipo"), rs.getString("telefono"));

    public JdbcProveedorDao(JdbcTemplate jdbc) {
        super(jdbc);
    }

    @Override
    public List<Proveedor> listar() {
        return jdbc.query(SELECT + "ORDER BY tipo DESC, nombre", MAPPER);
    }

    @Override
    public Optional<Proveedor> buscar(int idProveedor) {
        return jdbc.query(SELECT + "WHERE id_proveedor = ?", MAPPER, idProveedor).stream().findFirst();
    }

    @Override
    public boolean existeNombre(String nombre) {
        return entero("SELECT COUNT(*) FROM proveedor WHERE nombre = ?", nombre) > 0;
    }

    @Override
    public int insertar(Proveedor p) {
        return (int) insertarConLlave("INSERT INTO proveedor (nombre, ruc, tipo, telefono) VALUES (?, ?, ?, ?)",
                p.nombre(), p.ruc(), p.tipo(), p.telefono());
    }
}
