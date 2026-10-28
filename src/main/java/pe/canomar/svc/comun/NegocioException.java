package pe.canomar.svc.comun;

/** Error de una regla del negocio: se muestra al usuario como mensaje, no como error técnico. */
public class NegocioException extends RuntimeException {
    public NegocioException(String mensaje) {
        super(mensaje);
    }
}
