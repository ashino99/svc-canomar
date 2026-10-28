package pe.canomar.svc.modelo;

/** Resultado de sincronizar una venta guardada en el celular (RF25). */
public record ResultadoSync(String uuid, String estado, String mensaje) {
}
