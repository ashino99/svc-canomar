package pe.canomar.svc.dao.jdbc;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import pe.canomar.svc.dao.ProductoDao;
import pe.canomar.svc.modelo.Producto;

import java.util.List;
import java.util.Optional;

@Repository
public class JdbcProductoDao extends JdbcBase implements ProductoDao {

    private static final String SELECT = "SELECT id_producto, nombre, nombre_corto, categoria, estado FROM producto ";
    private static final RowMapper<Producto> MAPPER = (rs, i) -> new Producto(rs.getInt("id_producto"), rs.getString("nombre"),
            rs.getString("nombre_corto"), rs.getString("categoria"), rs.getString("estado"));

    public JdbcProductoDao(JdbcTemplate jdbc) {
        super(jdbc);
    }

    @Override
    public List<Producto> listar() {
        return jdbc.query(SELECT + "ORDER BY estado, nombre", MAPPER);
    }

    @Override
    public List<Producto> listarActivos() {
        return jdbc.query(SELECT + "WHERE estado = 'ACTIVO' ORDER BY nombre", MAPPER);
    }

    @Override
    public Optional<Producto> buscar(int idProducto) {
        return jdbc.query(SELECT + "WHERE id_producto = ?", MAPPER, idProducto).stream().findFirst();
    }

    @Override
    public boolean existeNombre(String nombre, Integer excluirId) {
        return entero("SELECT COUNT(*) FROM producto WHERE nombre = ? AND id_producto <> ?", nombre, excluirId == null ? 0 : excluirId) > 0;
    }

    @Override
    public int insertar(Producto p) {
        return (int) insertarConLlave("INSERT INTO producto (nombre, nombre_corto, categoria, estado) VALUES (?, ?, ?, ?)",
                p.nombre(), p.nombreCorto(), p.categoria(), p.estado());
    }

    @Override
    public void actualizar(Producto p) {
        jdbc.update("UPDATE producto SET nombre = ?, nombre_corto = ?, categoria = ?, estado = ? WHERE id_producto = ?",
                p.nombre(), p.nombreCorto(), p.categoria(), p.estado(), p.id());
    }
}
