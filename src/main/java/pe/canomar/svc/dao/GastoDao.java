package pe.canomar.svc.dao;

import pe.canomar.svc.modelo.GastoVista;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Acceso a gasto. */
public interface GastoDao {
    int insertar(int idUsuario, LocalDate fecha, String concepto, String descripcion, BigDecimal monto, String medio);

    List<GastoVista> listarPorFecha(LocalDate fecha);

    BigDecimal total(LocalDate desde, LocalDate hasta);

    BigDecimal totalDelDia(LocalDate fecha, boolean efectivo);
}
