package pe.canomar.svc.servicio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.canomar.svc.comun.NegocioException;
import pe.canomar.svc.dao.UsuarioDao;
import pe.canomar.svc.modelo.Usuario;
import pe.canomar.svc.servicio.impl.UsuarioServiceImpl;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas de usuarios y contraseñas (RF01, RF04, RF05) y del cifrado con BCrypt (RNF15). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UsuarioServiceImplTest {

    @Mock UsuarioDao dao;
    @Mock AuditoriaService auditoria;
    PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    UsuarioServiceImpl servicio;

    @BeforeEach
    void preparar() {
        servicio = new UsuarioServiceImpl(dao, encoder, auditoria);
    }

    @Test
    @DisplayName("RF01: la contraseña se guarda cifrada, nunca en texto plano")
    void registraConBcrypt() {
        servicio.registrar("Rosa Canales", "Contador", "CONTADOR", "Canomar2026", 1);
        verify(dao).insertar(eq("Rosa Canales"), eq("contador"), org.mockito.ArgumentMatchers.argThat(h -> h.startsWith("$2") && encoder.matches("Canomar2026", h)), eq("CONTADOR"));
    }

    @Test
    @DisplayName("RF01 alterno: usuario duplicado")
    void usuarioDuplicado() {
        when(dao.existeUsuario("vendedor")).thenReturn(true);
        assertThrows(NegocioException.class, () -> servicio.registrar("Otro", "vendedor", "VENDEDOR", "Canomar2026", 1));
    }

    @Test
    @DisplayName("RF01 alterno: contraseña de menos de 8 caracteres")
    void claveCorta() {
        NegocioException e = assertThrows(NegocioException.class, () -> servicio.registrar("Ana", "ana", "VENDEDOR", "1234", 1));
        assertTrue(e.getMessage().contains("8"));
    }

    @Test
    @DisplayName("RF04: la contraseña actual debe ser correcta")
    void cambiarClaveActualIncorrecta() {
        String hash = encoder.encode("Canomar2026");
        when(dao.buscarPorId(2)).thenReturn(Optional.of(new Usuario(2, "Vendedor", "vendedor", hash, "VENDEDOR", "ACTIVO", 0, null, LocalDateTime.now())));
        assertThrows(NegocioException.class, () -> servicio.cambiarClave(2, "otra", "NuevaClave1", "NuevaClave1"));
        assertThrows(NegocioException.class, () -> servicio.cambiarClave(2, "Canomar2026", "NuevaClave1", "NuevaClave2"));
        servicio.cambiarClave(2, "Canomar2026", "NuevaClave1", "NuevaClave1");
        verify(dao).actualizarClave(eq(2), anyString());
        assertNotEquals(hash, encoder.encode("NuevaClave1"));
    }

    @Test
    @DisplayName("RF05 alterno: no se puede desactivar al único administrador")
    void ultimoAdministrador() {
        when(dao.buscarPorId(3)).thenReturn(Optional.of(new Usuario(3, "Admin", "admin2", "x", "ADMINISTRADOR", "ACTIVO", 0, null, LocalDateTime.now())));
        when(dao.contarAdministradoresActivos()).thenReturn(1);
        assertThrows(NegocioException.class, () -> servicio.desactivar(3, 1));
    }
}
