package pe.canomar.svc.servicio.impl;

import org.springframework.stereotype.Service;
import pe.canomar.svc.comun.Dinero;
import pe.canomar.svc.comun.Fechas;
import pe.canomar.svc.modelo.DeudaProveedor;
import pe.canomar.svc.modelo.Ganancia;
import pe.canomar.svc.modelo.Panel;
import pe.canomar.svc.modelo.SaldoCliente;
import pe.canomar.svc.modelo.SaldoLinea;
import pe.canomar.svc.servicio.CajaService;
import pe.canomar.svc.servicio.CompraService;
import pe.canomar.svc.servicio.CreditoService;
import pe.canomar.svc.servicio.PanelService;
import pe.canomar.svc.servicio.SaldoService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class PanelServiceImpl implements PanelService {

    private final CajaService caja;
    private final CreditoService credito;
    private final CompraService compras;
    private final SaldoService saldo;

    public PanelServiceImpl(CajaService caja, CreditoService credito, CompraService compras, SaldoService saldo) {
        this.caja = caja;
        this.credito = credito;
        this.compras = compras;
        this.saldo = saldo;
    }

    @Override
    public Panel panel() {
        LocalDate hoy = Fechas.hoy();
        Ganancia g = caja.ganancia(hoy);
        List<SaldoCliente> saldos = credito.saldos();
        BigDecimal porCobrar = saldos.stream().map(SaldoCliente::saldo).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal deudaProv = compras.deudas().stream().map(DeudaProveedor::deuda).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<SaldoLinea> lineas = saldo.saldo();
        BigDecimal kilos = lineas.stream().map(l -> l.lote().kilos()).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<SaldoCliente> vencidos = saldos.stream().filter(s -> s.conVencido() || s.sobreLimite()).toList();
        return new Panel(g.ventas(), g.ventasCantidad(), g.ganancia(), Dinero.r2(porCobrar), Dinero.r2(deudaProv), Dinero.r2(kilos),
                saldo.mercaderiaAntigua(), vencidos);
    }
}
