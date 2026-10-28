package pe.canomar.svc.dao;

import pe.canomar.svc.modelo.CompromisoVista;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Acceso a compromiso_pago. */
public interface CompromisoDao {
    int insertar(int idCliente, LocalDate fecha, BigDecimal monto);

    List<CompromisoVista> listarVigentes();

    int marcarIncumplidos(LocalDate hoy);

    void marcarCumplidos(int idCliente);
}
