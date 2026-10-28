package pe.canomar.svc.dao;

import pe.canomar.svc.modelo.Producto;

import java.util.List;
import java.util.Optional;

/** Acceso a la tabla producto. */
public interface ProductoDao {
    List<Producto> listar();

    List<Producto> listarActivos();

    Optional<Producto> buscar(int idProducto);

    boolean existeNombre(String nombre, Integer excluirId);

    int insertar(Producto producto);

    void actualizar(Producto producto);
}
