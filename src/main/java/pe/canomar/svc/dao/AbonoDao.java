package pe.canomar.svc.dao;

import pe.canomar.svc.modelo.AbonoVista;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Acceso a abono y abono_venta. */
public interface AbonoDao {
    int insertar(int idCliente, int idUsuario, LocalDateTime fecha, BigDecimal monto, String medio);

    void aplicar(int idAbono, long idVenta, BigDecimal monto);

    List<AbonoVista> recientes(int limite);

    List<AbonoVista> abonosCliente(int idCliente, LocalDateTime desde, LocalDateTime hasta);

    BigDecimal abonosAntesDe(int idCliente, LocalDateTime antes);

    BigDecimal abonosDelDia(LocalDate fecha, boolean efectivo);
}
