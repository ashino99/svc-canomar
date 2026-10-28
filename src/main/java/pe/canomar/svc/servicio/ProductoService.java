package pe.canomar.svc.servicio;

import pe.canomar.svc.modelo.Producto;

import java.util.List;

/** Catálogo de productos (RF06, RF07). */
public interface ProductoService {
    List<Producto> listar();

    List<Producto> activos();

    Producto buscar(int idProducto);

    int registrar(String nombre, String nombreCorto, String categoria, int idActor);

    void editar(int idProducto, String nombre, String nombreCorto, String categoria, String estado, int idActor);
}
