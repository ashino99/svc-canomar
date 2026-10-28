package pe.canomar.svc.modelo;

/** Especie que se vende por kilo (tabla producto). */
public record Producto(Integer id, String nombre, String nombreCorto, String categoria, String estado) {
    public boolean activo() {
        return "ACTIVO".equals(estado);
    }
}
