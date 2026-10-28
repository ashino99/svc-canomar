package pe.canomar.svc.servicio.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.canomar.svc.comun.NegocioException;
import pe.canomar.svc.dao.UsuarioDao;
import pe.canomar.svc.modelo.Usuario;
import pe.canomar.svc.servicio.AuditoriaService;
import pe.canomar.svc.servicio.UsuarioService;

import java.util.List;
import java.util.Set;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    static final int LONGITUD_MINIMA = 8;
    private static final Set<String> ROLES = Set.of("VENDEDOR", "ADMINISTRADOR", "CONTADOR");

    private final UsuarioDao dao;
    private final PasswordEncoder encoder;
    private final AuditoriaService auditoria;

    public UsuarioServiceImpl(UsuarioDao dao, PasswordEncoder encoder, AuditoriaService auditoria) {
        this.dao = dao;
        this.encoder = encoder;
        this.auditoria = auditoria;
    }

    @Override
    public List<Usuario> listar() {
        return dao.listar();
    }

    @Override
    @Transactional
    public int registrar(String nombreCompleto, String usuario, String rol, String clave, int idActor) {
        String nombre = StringUtils.normalizeSpace(nombreCompleto);
        String login = StringUtils.lowerCase(StringUtils.trimToEmpty(usuario));
        if (StringUtils.isAnyBlank(nombre, login)) {
            throw new NegocioException("Ingrese el nombre y el usuario");
        }
        if (!login.matches("[a-z0-9._]{3,30}")) {
            throw new NegocioException("El usuario debe tener de 3 a 30 letras, números, punto o guion bajo");
        }
        if (!ROLES.contains(rol)) {
            throw new NegocioException("Elija un rol válido");
        }
        validarClave(clave);
        if (dao.existeUsuario(login)) {
            throw new NegocioException("El usuario ya existe, ingrese otro");
        }
        int id = dao.insertar(nombre, login, encoder.encode(clave), rol);
        auditoria.registrar(idActor, "REGISTRAR_USUARIO", login + " · " + rol);
        return id;
    }

    @Override
    @Transactional
    public void desactivar(int idUsuario, int idActor) {
        Usuario u = dao.buscarPorId(idUsuario).orElseThrow(() -> new NegocioException("El usuario no existe"));
        if (u.id() == idActor) {
            throw new NegocioException("No puede desactivar su propia cuenta");
        }
        if ("ADMINISTRADOR".equals(u.rol()) && u.activo() && dao.contarAdministradoresActivos() <= 1) {
            throw new NegocioException("No puede desactivar al único administrador");
        }
        dao.cambiarEstado(idUsuario, "INACTIVO");
        auditoria.registrar(idActor, "DESACTIVAR_USUARIO", u.usuario());
    }

    @Override
    @Transactional
    public void activar(int idUsuario, int idActor) {
        Usuario u = dao.buscarPorId(idUsuario).orElseThrow(() -> new NegocioException("El usuario no existe"));
        dao.cambiarEstado(idUsuario, "ACTIVO");
        auditoria.registrar(idActor, "ACTIVAR_USUARIO", u.usuario());
    }

    @Override
    @Transactional
    public void cambiarClave(int idUsuario, String actual, String nueva, String repetir) {
        if (!StringUtils.equals(nueva, repetir)) {
            throw new NegocioException("Las contraseñas no coinciden");
        }
        validarClave(nueva);
        Usuario u = dao.buscarPorId(idUsuario).orElseThrow(() -> new NegocioException("El usuario no existe"));
        if (actual == null || !encoder.matches(actual, u.claveHash())) {
            throw new NegocioException("La contraseña actual es incorrecta");
        }
        dao.actualizarClave(idUsuario, encoder.encode(nueva));
        auditoria.registrar(idUsuario, "CAMBIAR_CLAVE", u.usuario());
    }

    private void validarClave(String clave) {
        if (clave == null || clave.length() < LONGITUD_MINIMA) {
            throw new NegocioException("La contraseña debe tener al menos " + LONGITUD_MINIMA + " caracteres");
        }
    }
}
