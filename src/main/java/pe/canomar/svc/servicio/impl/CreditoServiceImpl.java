package pe.canomar.svc.servicio.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.canomar.svc.comun.Calculos;
import pe.canomar.svc.comun.Calculos.Aplicacion;
import pe.canomar.svc.comun.Calculos.Pendiente;
import pe.canomar.svc.comun.Dinero;
import pe.canomar.svc.comun.Fechas;
import pe.canomar.svc.comun.NegocioException;
import pe.canomar.svc.dao.AbonoDao;
import pe.canomar.svc.dao.ClienteDao;
import pe.canomar.svc.dao.CompromisoDao;
import pe.canomar.svc.dao.VentaDao;
import pe.canomar.svc.modelo.AbonoVista;
import pe.canomar.svc.modelo.Antiguedad;
import pe.canomar.svc.modelo.Cliente;
import pe.canomar.svc.modelo.CompromisoVista;
import pe.canomar.svc.modelo.EstadoCuenta;
import pe.canomar.svc.modelo.Movimiento;
import pe.canomar.svc.modelo.PendienteVenta;
import pe.canomar.svc.modelo.SaldoCliente;
import pe.canomar.svc.modelo.VentaVista;
import pe.canomar.svc.servicio.AuditoriaService;
import pe.canomar.svc.servicio.CreditoService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CreditoServiceImpl implements CreditoService {

    private static final Logger log = LoggerFactory.getLogger(CreditoServiceImpl.class);
    private static final List<String> TRAMOS = List.of("0–7 días", "8–15 días", "16–30 días", "Más de 30 días");

    private final VentaDao ventaDao;
    private final AbonoDao abonoDao;
    private final ClienteDao clienteDao;
    private final CompromisoDao compromisoDao;
    private final AuditoriaService auditoria;

    public CreditoServiceImpl(VentaDao ventaDao, AbonoDao abonoDao, ClienteDao clienteDao, CompromisoDao compromisoDao, AuditoriaService auditoria) {
        this.ventaDao = ventaDao;
        this.abonoDao = abonoDao;
        this.clienteDao = clienteDao;
        this.compromisoDao = compromisoDao;
        this.auditoria = auditoria;
    }

    @Override
    public List<SaldoCliente> saldos() {
        return ventaDao.saldosClientes(Fechas.hoy());
    }

    @Override
    public SaldoCliente saldo(int idCliente) {
        Cliente c = cliente(idCliente);
        LocalDate hoy = Fechas.hoy();
        BigDecimal saldo = BigDecimal.ZERO;
        BigDecimal vencido = BigDecimal.ZERO;
        LocalDate antigua = null;
        for (PendienteVenta p : ventaDao.pendientesCliente(idCliente)) {
            if (!Dinero.positivo(p.pendiente())) {
                continue;
            }
            saldo = saldo.add(p.pendiente());
            LocalDate f = p.fechaHora().toLocalDate();
            if (Fechas.diasEntre(f, hoy) > c.plazoDias()) {
                vencido = vencido.add(p.pendiente());
            }
            if (antigua == null) {
                antigua = f;
            }
        }
        return new SaldoCliente(c.id(), c.nombreComercial(), c.ruc(), c.limiteCredito(), c.plazoDias(), Dinero.r2(saldo), Dinero.r2(vencido), antigua);
    }

    @Override
    public List<PendienteVenta> pendientes(int idCliente) {
        return ventaDao.pendientesCliente(idCliente);
    }

    @Override
    @Transactional
    public BigDecimal registrarAbono(int idCliente, BigDecimal monto, String medio, int idActor) {
        BigDecimal m = Validar.montoPositivo(monto, "Ingrese un monto mayor que cero");
        Validar.medio(medio);
        SaldoCliente s = saldo(idCliente);
        if (m.compareTo(s.saldo()) > 0) {
            throw new NegocioException("El abono supera el saldo (S/ " + s.saldo() + ")");
        }
        int idAbono = abonoDao.insertar(idCliente, idActor, Fechas.ahora(), m, medio);
        List<Pendiente> pend = ventaDao.pendientesCliente(idCliente).stream()
                .map(p -> new Pendiente(p.idVenta(), p.pendiente())).toList();
        for (Aplicacion a : Calculos.aplicarAbono(pend, m)) {
            abonoDao.aplicar(idAbono, a.idVenta(), a.monto());
        }
        BigDecimal nuevo = Dinero.r2(s.saldo().subtract(m));
        if (nuevo.signum() == 0) {
            compromisoDao.marcarCumplidos(idCliente);
        }
        auditoria.registrar(idActor, "REGISTRAR_ABONO", s.nombre() + " · S/ " + m + " · " + medio + " · saldo S/ " + nuevo);
        log.info("Abono de S/ {} a {}; nuevo saldo S/ {}", m, s.nombre(), nuevo);
        return nuevo;
    }

    @Override
    public List<Antiguedad> antiguedad(int idCliente) {
        cliente(idCliente);
        LocalDate hoy = Fechas.hoy();
        Map<String, BigDecimal> montos = new LinkedHashMap<>();
        Map<String, Integer> cantidad = new LinkedHashMap<>();
        TRAMOS.forEach(t -> {
            montos.put(t, BigDecimal.ZERO);
            cantidad.put(t, 0);
        });
        for (PendienteVenta p : ventaDao.pendientesCliente(idCliente)) {
            if (!Dinero.positivo(p.pendiente())) {
                continue;
            }
            String t = Calculos.tramo(Fechas.diasEntre(p.fechaHora().toLocalDate(), hoy));
            montos.merge(t, p.pendiente(), BigDecimal::add);
            cantidad.merge(t, 1, Integer::sum);
        }
        return TRAMOS.stream().map(t -> new Antiguedad(t, Dinero.r2(montos.get(t)), cantidad.get(t))).toList();
    }

    @Override
    public List<SaldoCliente> vencidos() {
        return saldos().stream().filter(s -> s.conVencido() || s.sobreLimite()).toList();
    }

    @Override
    @Transactional
    public void registrarCompromiso(int idCliente, LocalDate fecha, BigDecimal monto, int idActor) {
        if (fecha == null || fecha.isBefore(Fechas.hoy())) {
            throw new NegocioException("La fecha debe ser hoy o posterior");
        }
        BigDecimal m = Validar.montoPositivo(monto, "Ingrese un monto mayor que cero");
        SaldoCliente s = saldo(idCliente);
        if (!Dinero.positivo(s.saldo())) {
            throw new NegocioException("El cliente no tiene deudas");
        }
        compromisoDao.insertar(idCliente, fecha, m);
        auditoria.registrar(idActor, "COMPROMISO_PAGO", s.nombre() + " · S/ " + m + " para el " + fecha.format(Fechas.DIA));
    }

    @Override
    @Transactional
    public List<CompromisoVista> compromisos() {
        compromisoDao.marcarIncumplidos(Fechas.hoy());
        return compromisoDao.listarVigentes();
    }

    @Override
    public EstadoCuenta estadoCuenta(int idCliente, LocalDate desde, LocalDate hasta) {
        Cliente c = cliente(idCliente);
        if (desde == null || hasta == null || desde.isAfter(hasta)) {
            throw new NegocioException("Revise el rango de fechas");
        }
        if (Fechas.diasEntre(desde, hasta) > 366) {
            throw new NegocioException("Elija un periodo de hasta un año");
        }
        LocalDateTime ini = desde.atStartOfDay();
        LocalDateTime fin = hasta.plusDays(1).atStartOfDay();
        BigDecimal inicial = Dinero.r2(ventaDao.ventasCreditoAntesDe(idCliente, ini).subtract(abonoDao.abonosAntesDe(idCliente, ini)));
        List<Movimiento> movs = new ArrayList<>();
        for (VentaVista v : ventaDao.ventasCreditoCliente(idCliente, ini, fin)) {
            movs.add(new Movimiento(v.fechaHora(), "Venta " + v.numero() + (v.esRemate() ? " (remate)" : ""), v.total(), null, null));
        }
        for (AbonoVista a : abonoDao.abonosCliente(idCliente, ini, fin)) {
            movs.add(new Movimiento(a.fecha(), "Abono · " + a.medio(), null, a.monto(), null));
        }
        movs.sort(Comparator.comparing(Movimiento::fecha));
        List<Movimiento> conSaldo = new ArrayList<>();
        BigDecimal saldo = inicial;
        for (Movimiento m : movs) {
            saldo = saldo.add(Dinero.nz(m.cargo())).subtract(Dinero.nz(m.abono()));
            conSaldo.add(new Movimiento(m.fecha(), m.descripcion(), m.cargo(), m.abono(), Dinero.r2(saldo)));
        }
        return new EstadoCuenta(c, desde, hasta, inicial, conSaldo, Dinero.r2(saldo));
    }

    private Cliente cliente(int idCliente) {
        return clienteDao.buscar(idCliente).orElseThrow(() -> new NegocioException("El cliente no existe"));
    }
}
