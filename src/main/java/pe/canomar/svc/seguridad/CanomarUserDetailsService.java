package pe.canomar.svc.seguridad;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import pe.canomar.svc.dao.UsuarioDao;

/** Carga el usuario desde MySQL para Spring Security (RF02). */
@Service
public class CanomarUserDetailsService implements UserDetailsService {

    private final UsuarioDao usuarioDao;

    public CanomarUserDetailsService(UsuarioDao usuarioDao) {
        this.usuarioDao = usuarioDao;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return usuarioDao.buscarPorUsuario(username.trim())
                .map(UsuarioSesion::new)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario o contraseña incorrectos"));
    }
}
