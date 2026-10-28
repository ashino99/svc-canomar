package pe.canomar.svc.dao.jdbc;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import pe.canomar.svc.comun.Fechas;
import pe.canomar.svc.dao.UsuarioDao;
import pe.canomar.svc.modelo.Usuario;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcUsuarioDao extends JdbcBase implements UsuarioDao {

    private static final String SELECT = "SELECT id_usuario, nombre_completo, usuario, clave_hash, rol, estado, intentos_fallidos, bloqueado_hasta, fecha_registro FROM usuario ";
    private static final RowMapper<Usuario> MAPPER = (rs, i) -> new Usuario(rs.getInt("id_usuario"), rs.getString("nombre_completo"),
            rs.getString("usuario"), rs.getString("clave_hash"), rs.getString("rol"), rs.getString("estado"),
            rs.getInt("intentos_fallidos"), fechaHora(rs, "bloqueado_hasta"), fechaHora(rs, "fecha_registro"));

    public JdbcUsuarioDao(JdbcTemplate jdbc) {
        super(jdbc);
    }

    @Override
    public Optional<Usuario> buscarPorUsuario(String usuario) {
        return jdbc.query(SELECT + "WHERE usuario = ?", MAPPER, usuario).stream().findFirst();
    }

    @Override
    public Optional<Usuario> buscarPorId(int idUsuario) {
        return jdbc.query(SELECT + "WHERE id_usuario = ?", MAPPER, idUsuario).stream().findFirst();
    }

    @Override
    public List<Usuario> listar() {
        return jdbc.query(SELECT + "ORDER BY estado, rol, usuario", MAPPER);
    }

    @Override
    public boolean existeUsuario(String usuario) {
        return entero("SELECT COUNT(*) FROM usuario WHERE usuario = ?", usuario) > 0;
    }

    @Override
    public int insertar(String nombreCompleto, String usuario, String claveHash, String rol) {
        return (int) insertarConLlave("INSERT INTO usuario (nombre_completo, usuario, clave_hash, rol, estado, intentos_fallidos, fecha_registro) VALUES (?, ?, ?, ?, 'ACTIVO', 0, ?)",
                nombreCompleto, usuario, claveHash, rol, Fechas.ahora());
    }

    @Override
    public void cambiarEstado(int idUsuario, String estado) {
        jdbc.update("UPDATE usuario SET estado = ? WHERE id_usuario = ?", estado, idUsuario);
    }

    @Override
    public void actualizarClave(int idUsuario, String claveHash) {
        jdbc.update("UPDATE usuario SET clave_hash = ? WHERE id_usuario = ?", claveHash, idUsuario);
    }

    @Override
    public void actualizarIntentos(int idUsuario, int intentos, LocalDateTime bloqueadoHasta) {
        jdbc.update("UPDATE usuario SET intentos_fallidos = ?, bloqueado_hasta = ? WHERE id_usuario = ?", intentos, bloqueadoHasta, idUsuario);
    }

    @Override
    public int contarAdministradoresActivos() {
        return entero("SELECT COUNT(*) FROM usuario WHERE rol = 'ADMINISTRADOR' AND estado = 'ACTIVO'");
    }
}
