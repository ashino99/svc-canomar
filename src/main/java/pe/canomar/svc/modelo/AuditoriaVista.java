package pe.canomar.svc.modelo;

import java.time.LocalDateTime;

/** Evento de la bitácora (RF50). */
public record AuditoriaVista(long id, LocalDateTime fechaHora, String usuario, String operacion, String detalle) {
}
