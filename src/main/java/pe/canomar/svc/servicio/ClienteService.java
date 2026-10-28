package pe.canomar.svc.servicio;

import pe.canomar.svc.modelo.Cliente;

import java.math.BigDecimal;
import java.util.List;

/** Catálogo de clientes (RF08, RF09). */
public interface ClienteService {
    List<Cliente> listar();

    List<Cliente> activos();

    Cliente buscar(int idCliente);

    int registrar(String nombre, String ruc, String telefono, BigDecimal limite, int plazoDias, int idActor);

    /** Devuelve una advertencia si el saldo actual queda por encima del nuevo límite; si no, null. */
    String editar(int idCliente, String nombre, String ruc, String telefono, BigDecimal limite, int plazoDias, String estado, int idActor);
}
