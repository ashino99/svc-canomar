package pe.canomar.svc.servicio;

import pe.canomar.svc.modelo.Usuario;

import java.util.List;

/** Gestión de usuarios y contraseñas (RF01, RF04, RF05). */
public interface UsuarioService {
    List<Usuario> listar();

    int registrar(String nombreCompleto, String usuario, String rol, String clave, int idActor);

    void desactivar(int idUsuario, int idActor);

    void activar(int idUsuario, int idActor);

    void cambiarClave(int idUsuario, String actual, String nueva, String repetir);
}
