package pe.canomar.svc.modelo;

/** Lote con su antigüedad en días y su color (RF28). */
public record SaldoLinea(LoteVista lote, long dias, String color) {
}
