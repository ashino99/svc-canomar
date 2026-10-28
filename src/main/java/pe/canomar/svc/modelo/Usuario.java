package pe.canomar.svc.modelo;

import java.time.LocalDateTime;

/** Persona que usa el sistema con un rol (tabla usuario). */
public record Usuario(Integer id, String nombreCompleto, String usuario, String claveHash, String rol, String estado, int intentosFallidos, LocalDateTime bloqueadoHasta, LocalDateTime fechaRegistro) {
    public boolean activo() {
        return "ACTIVO".equals(estado);
    }
}
