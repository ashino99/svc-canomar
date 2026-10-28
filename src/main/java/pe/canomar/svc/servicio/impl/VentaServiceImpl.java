package pe.canomar.svc.servicio.impl;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import pe.canomar.svc.comun.Calculos;
import pe.canomar.svc.comun.Calculos.Consumo;
import pe.canomar.svc.comun.Calculos.ResultadoFifo;
import pe.canomar.svc.comun.ConfirmacionException;
import pe.canomar.svc.comun.Dinero;
import pe.canomar.svc.comun.Fechas;
import pe.canomar.svc.comun.NegocioException;
import pe.canomar.svc.dao.ClienteDao;
import pe.canomar.svc.dao.LoteDao;
import pe.canomar.svc.dao.PrecioDao;
import pe.canomar.svc.dao.ProductoDao;
import pe.canomar.svc.dao.VentaDao;
import pe.canomar.svc.modelo.Cliente;
import pe.canomar.svc.modelo.DetalleVentaVista;
import pe.canomar.svc.modelo.ItemVenta;
import pe.canomar.svc.modelo.NuevaVenta;
import pe.canomar.svc.modelo.PendienteVenta;
import pe.canomar.svc.modelo.Producto;
import pe.canomar.svc.modelo.ResultadoSync;
import pe.canomar.svc.modelo.SaldoCliente;
import pe.canomar.svc.modelo.VentaVista;
import pe.canomar.svc.servicio.AuditoriaService;
import pe.canomar.svc.servicio.CreditoService;
import pe.canomar.svc.servicio.VentaService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Lógica de la venta por kilo. El importe siempre lo calcula el sistema (RF19); los kilos se descuentan
 * del saldo por FIFO y cada línea guarda su costo para calcular la ganancia real (RF40).
 */
@Service
public class VentaServiceImpl implements VentaService {

    private static final Logger log = LoggerFactory.getLogger(VentaServiceImpl.class);

    private final VentaDao ventaDao;
    private final ProductoDao productoDao;
    private final ClienteDao clienteDao;
    private final PrecioDao precioDao;
    private final LoteDao loteDao;
    private final CreditoService credito;
    private final AuditoriaService auditoria;
    private final TransactionTemplate tx;

    public VentaServiceImpl(VentaDao ventaDao, ProductoDao productoDao, ClienteDao clienteDao, PrecioDao precioDao, LoteDao loteDao,
                            CreditoService credito, AuditoriaService auditoria, PlatformTransactionManager txManager) {
        this.ventaDao = ventaDao;
        this.productoDao = productoDao;
        this.clienteDao = clienteDao;
        this.precioDao = precioDao;
        this.loteDao = loteDao;
        this.credito = credito;
        this.auditoria = auditoria;
        this.tx = new TransactionTemplate(txManager);
    }

    @Override
    @Transactional
    public VentaVista registrar(NuevaVenta venta, int idActor, boolean esAdministrador, boolean confirmado) {
        long id = registrarInterno(venta, idActor, esAdministrador, confirmado, null);
        return buscar(id);
    }

    @Override
    public List<ResultadoSync> sincronizar(List<NuevaVenta> ventas, int idActor) {
        List<ResultadoSync> out = new ArrayList<>();
        for (NuevaVenta v : ventas) {
            if (StringUtils.isBlank(v.uuid())) {
                out.add(new ResultadoSync(v.uuid(), "RECHAZADA", "La venta no tiene identificador"));
                continue;
            }
            if (ventaDao.existeUuid(v.uuid())) {
                out.add(new ResultadoSync(v.uuid(), "DUPLICADA", "Ya estaba registrada"));
                continue;
            }
            try {
                // Cada venta en su propia transacción: si una falla, las demás se guardan.
                Long id = tx.execute(s -> registrarInterno(v, idActor, true, true, Fechas.ahora()));
                out.add(new ResultadoSync(v.uuid(), "SINCRONIZADA", "Venta " + String.format("%04d", id)));
            } catch (NegocioException e) {
                out.add(new ResultadoSync(v.uuid(), "RECHAZADA", e.getMessage()));
                auditoria.registrar(idActor, "VENTA_RECHAZADA", v.uuid() + ": " + e.getMessage());
            }
        }
        long ok = out.stream().filter(r -> "SINCRONIZADA".equals(r.estado())).count();
        if (ok > 0) {
            auditoria.registrar(idActor, "SINCRONIZAR_VENTAS", ok + " ventas guardadas sin conexión");
        }
        return out;
    }

    private long registrarInterno(NuevaVenta v, int idActor, boolean esAdministrador, boolean confirmado, LocalDateTime sincronizada) {
        LocalDate hoy = Fechas.hoy();
        if (v.items() == null || v.items().isEmpty()) {
            throw new NegocioException("Agregue al menos un producto");
        }
        boolean esCredito = "CREDITO".equals(v.tipoPago());
        if (!esCredito && !"CONTADO".equals(v.tipoPago())) {
            throw new NegocioException("Indique si la venta es al contado o al crédito");
        }
        String medio = esCredito ? null : StringUtils.defaultIfBlank(v.medioPago(), "EFECTIVO");
        if (medio != null) {
            Validar.medio(medio);
        }
        List<String> avisos = new ArrayList<>();
        List<ItemVenta> items = new ArrayList<>();
        for (ItemVenta it : v.items()) {
            Producto p = productoDao.buscar(it.idProducto()).orElseThrow(() -> new NegocioException("Producto no válido"));
            if (!p.activo()) {
                throw new NegocioException(p.nombre() + " está inactivo");
            }
            if (!Dinero.positivo(it.kilos())) {
                throw new NegocioException(p.nombre() + ": ingrese los kilos de la balanza");
            }
            BigDecimal kilos = Calculos.kilos(it.kilos());
            BigDecimal precioDia = precioDao.precioDe(p.id(), hoy).orElse(null);
            BigDecimal precio = it.precioKg() != null ? Dinero.r2(it.precioKg()) : precioDia;
            if (!Dinero.positivo(precio)) {
                throw new NegocioException(p.nombre() + ": ingrese el precio de hoy de este producto");
            }
            if (v.esRemate() && precioDia != null && precio.compareTo(precioDia) >= 0) {
                throw new NegocioException(p.nombre() + ": el precio de remate debe ser menor al del día (S/ " + precioDia + ")");
            }
            BigDecimal disponible = loteDao.kilosDisponibles(p.id());
            if (kilos.compareTo(disponible) > 0) {
                avisos.add("Solo quedan " + disponible + " kg de " + p.nombre().toLowerCase());
            }
            items.add(new ItemVenta(p.id(), kilos, precio));
        }
        NuevaVenta limpia = new NuevaVenta(StringUtils.defaultIfBlank(v.uuid(), UUID.randomUUID().toString()),
                v.fechaHora() != null ? v.fechaHora() : Fechas.ahora(), v.tipoPago(), medio, esCredito ? v.idCliente() : null, v.esRemate(), items);
        BigDecimal total = limpia.total();

        if (esCredito) {
            if (v.idCliente() == null) {
                throw new NegocioException("Elija el restaurante para la venta al crédito");
            }
            Cliente c = clienteDao.buscar(v.idCliente()).filter(x -> "ACTIVO".equals(x.estado()))
                    .orElseThrow(() -> new NegocioException("El cliente no existe o está inactivo"));
            if (!Dinero.positivo(c.limiteCredito())) {
                throw new NegocioException("Este cliente solo puede comprar al contado");
            }
            SaldoCliente s = credito.saldo(c.id());
            if (total.compareTo(s.disponible()) > 0) {
                String msg = "Supera el límite de crédito: disponible S/ " + s.disponible() + " de S/ " + c.limiteCredito();
                if (!esAdministrador) {
                    throw new NegocioException(msg + ". Requiere autorización del administrador");
                }
                avisos.add(msg);
            }
            if (s.conVencido()) {
                avisos.add(c.nombreComercial() + " tiene deuda vencida de S/ " + s.vencido());
            }
        }
        if (!avisos.isEmpty() && !confirmado) {
            throw new ConfirmacionException(String.join(" · ", avisos) + ". ¿Registrar igual?");
        }
        if (ventaDao.existeUuid(limpia.uuid())) {
            throw new NegocioException("La venta ya fue registrada");
        }

        long idVenta = ventaDao.insertarVenta(limpia, idActor, sincronizada);
        for (ItemVenta it : items) {
            ResultadoFifo fifo = Calculos.consumirFifo(loteDao.disponiblesFifo(it.idProducto()), it.kilos());
            for (Consumo c : fifo.consumos()) {
                loteDao.descontar(c.idLote(), c.kilos());
            }
            BigDecimal costoRef = precioDao.ultimoCosto(it.idProducto(), hoy).orElse(BigDecimal.ZERO);
            BigDecimal costoKg = Calculos.costoKgVendido(fifo, it.kilos(), costoRef);
            ventaDao.insertarDetalle(idVenta, it.idProducto(), it.kilos(), it.precioKg(), costoKg);
        }
        String tipo = v.esRemate() ? "REMATE" : (esCredito ? "CREDITO" : "CONTADO · " + medio);
        auditoria.registrar(idActor, "REGISTRAR_VENTA", String.format("%04d", idVenta) + " · S/ " + total + " · " + tipo
                + (sincronizada != null ? " · sin conexión" : ""));
        log.info("Venta {} registrada: S/ {} ({})", idVenta, total, tipo);
        return idVenta;
    }

    @Override
    @Transactional
    public void anular(long idVenta, String motivo, int idActor) {
        VentaVista v = buscar(idVenta);
        if (v.anulada()) {
            throw new NegocioException("La venta ya está anulada");
        }
        if (StringUtils.isBlank(motivo)) {
            throw new NegocioException("Indique el motivo de la anulación");
        }
        if (v.idCliente() != null && "CREDITO".equals(v.tipoPago())) {
            PendienteVenta p = credito.pendientes(v.idCliente()).stream().filter(x -> x.idVenta() == idVenta).findFirst().orElse(null);
            if (p != null && Dinero.positivo(p.pagado())) {
                throw new NegocioException("La venta ya tiene abonos aplicados (S/ " + p.pagado() + "); no se puede anular");
            }
        }
        BigDecimal kilos = BigDecimal.ZERO;
        for (DetalleVentaVista d : ventaDao.detalle(idVenta)) {
            Integer lote = loteDao.ultimoLote(d.idProducto()).orElse(null);
            if (lote != null) {
                loteDao.sumar(lote, d.kilos());
            } else {
                loteDao.insertar(d.idProducto(), null, Fechas.hoy(), d.kilos(), d.costoKg());
            }
            kilos = kilos.add(d.kilos());
        }
        ventaDao.anular(idVenta, StringUtils.left(StringUtils.normalizeSpace(motivo), 150));
        auditoria.registrar(idActor, "ANULAR_VENTA", v.numero() + " · S/ " + v.total() + " · " + motivo
                + (v.idFactura() != null ? " · tenía factura: emitir nota de crédito en SUNAT" : ""));
        log.info("Venta {} anulada, se devolvieron {} kg", idVenta, kilos);
    }

    @Override
    public List<VentaVista> ventasDelDia(LocalDate fecha) {
        return ventaDao.listarEntre(fecha, fecha);
    }

    @Override
    public VentaVista buscar(long idVenta) {
        return ventaDao.buscar(idVenta).orElseThrow(() -> new NegocioException("La venta no existe"));
    }

    @Override
    public List<DetalleVentaVista> detalle(long idVenta) {
        return ventaDao.detalle(idVenta);
    }
}
