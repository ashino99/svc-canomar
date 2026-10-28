package pe.canomar.svc.servicio;

import pe.canomar.svc.modelo.Antiguedad;
import pe.canomar.svc.modelo.CompromisoVista;
import pe.canomar.svc.modelo.EstadoCuenta;
import pe.canomar.svc.modelo.PendienteVenta;
import pe.canomar.svc.modelo.SaldoCliente;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Créditos, abonos y cobranza (RF31–RF37). */
public interface CreditoService {
    List<SaldoCliente> saldos();

    SaldoCliente saldo(int idCliente);

    List<PendienteVenta> pendientes(int idCliente);

    /** RF31: registra el abono, lo aplica a las ventas más antiguas y devuelve el nuevo saldo. */
    BigDecimal registrarAbono(int idCliente, BigDecimal monto, String medio, int idActor);

    List<Antiguedad> antiguedad(int idCliente);

    List<SaldoCliente> vencidos();

    void registrarCompromiso(int idCliente, LocalDate fecha, BigDecimal monto, int idActor);

    List<CompromisoVista> compromisos();

    EstadoCuenta estadoCuenta(int idCliente, LocalDate desde, LocalDate hasta);
}
