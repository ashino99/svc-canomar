package pe.canomar.svc.dao;

import pe.canomar.svc.modelo.Proveedor;

import java.util.List;
import java.util.Optional;

/** Acceso a la tabla proveedor. */
public interface ProveedorDao {
    List<Proveedor> listar();

    Optional<Proveedor> buscar(int idProveedor);

    boolean existeNombre(String nombre);

    int insertar(Proveedor proveedor);
}
