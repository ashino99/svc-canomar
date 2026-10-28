package pe.canomar.svc.dao;

import pe.canomar.svc.modelo.Usuario;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Acceso a la tabla usuario (patrón DAO). */
public interface UsuarioDao {
    Optional<Usuario> buscarPorUsuario(String usuario);

    Optional<Usuario> buscarPorId(int idUsuario);

    List<Usuario> listar();

    boolean existeUsuario(String usuario);

    int insertar(String nombreCompleto, String usuario, String claveHash, String rol);

    void cambiarEstado(int idUsuario, String estado);

    void actualizarClave(int idUsuario, String claveHash);

    void actualizarIntentos(int idUsuario, int intentos, LocalDateTime bloqueadoHasta);

    int contarAdministradoresActivos();
}
