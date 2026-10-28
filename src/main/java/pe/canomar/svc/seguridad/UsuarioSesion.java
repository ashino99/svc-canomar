package pe.canomar.svc.seguridad;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import pe.canomar.svc.comun.Fechas;
import pe.canomar.svc.modelo.Usuario;

import java.util.Collection;
import java.util.List;

/** Usuario autenticado en la sesión: su id, su nombre y su rol (VENDEDOR, ADMINISTRADOR o CONTADOR). */
public class UsuarioSesion implements UserDetails {

    private final int id;
    private final String usuario;
    private final String nombre;
    private final String rol;
    private final String claveHash;
    private final boolean activo;
    private final boolean bloqueado;

    public UsuarioSesion(Usuario u) {
        this.id = u.id();
        this.usuario = u.usuario();
        this.nombre = u.nombreCompleto();
        this.rol = u.rol();
        this.claveHash = u.claveHash();
        this.activo = u.activo();
        this.bloqueado = u.bloqueadoHasta() != null && u.bloqueadoHasta().isAfter(Fechas.ahora());
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getRol() {
        return rol;
    }

    public boolean esAdministrador() {
        return "ADMINISTRADOR".equals(rol);
    }

    public String getRolTexto() {
        return switch (rol) {
            case "ADMINISTRADOR" -> "Administrador del negocio";
            case "CONTADOR" -> "Contador";
            default -> "Vendedor";
        };
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    @Override
    public String getPassword() {
        return claveHash;
    }

    @Override
    public String getUsername() {
        return usuario;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !bloqueado;
    }

    @Override
    public boolean isEnabled() {
        return activo;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
