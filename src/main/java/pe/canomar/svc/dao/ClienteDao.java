package pe.canomar.svc.dao;

import pe.canomar.svc.modelo.Cliente;

import java.util.List;
import java.util.Optional;

/** Acceso a la tabla cliente. */
public interface ClienteDao {
    List<Cliente> listar();

    List<Cliente> listarActivos();

    Optional<Cliente> buscar(int idCliente);

    boolean existeRuc(String ruc, Integer excluirId);

    int insertar(Cliente cliente);

    void actualizar(Cliente cliente);
}
