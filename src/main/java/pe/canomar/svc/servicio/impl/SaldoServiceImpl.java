package pe.canomar.svc.servicio.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.canomar.svc.comun.Calculos;
import pe.canomar.svc.comun.Calculos.Consumo;
import pe.canomar.svc.comun.Calculos.ResultadoFifo;
import pe.canomar.svc.comun.Dinero;
import pe.canomar.svc.comun.Fechas;
import pe.canomar.svc.comun.NegocioException;
import pe.canomar.svc.dao.ConfiguracionDao;
import pe.canomar.svc.dao.LoteDao;
import pe.canomar.svc.dao.PrecioDao;
import pe.canomar.svc.dao.ProductoDao;
import pe.canomar.svc.modelo.Configuracion;
import pe.canomar.svc.modelo.Producto;
import pe.canomar.svc.modelo.SaldoLinea;
import pe.canomar.svc.servicio.AuditoriaService;
import pe.canomar.svc.servicio.SaldoService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SaldoServiceImpl implements SaldoService {

    private static final Set<String> TIPOS_AJUSTE = Set.of("MERMA", "DESECHO", "CORRECCION");

    private final LoteDao loteDao;
    private final ProductoDao productoDao;
    private final PrecioDao precioDao;
    private final ConfiguracionDao configDao;
    private final AuditoriaService auditoria;

    public SaldoServiceImpl(LoteDao loteDao, ProductoDao productoDao, PrecioDao precioDao, ConfiguracionDao configDao, AuditoriaService auditoria) {
        this.loteDao = loteDao;
        this.productoDao = productoDao;
        this.precioDao = precioDao;
        this.configDao = configDao;
        this.auditoria = auditoria;
    }

    @Override
    public List<SaldoLinea> saldo() {
        LocalDate hoy = Fechas.hoy();
        int diasAlerta = configDao.obtener().diasAlertaSaldo();
        return loteDao.lotesConKilos().stream().map(l -> {
            long dias = Fechas.diasEntre(l.fechaIngreso(), hoy);
            return new SaldoLinea(l, dias, Calculos.colorSaldo(dias, diasAlerta));
        }).toList();
    }

    @Override
    public List<SaldoLinea> mercaderiaAntigua() {
        int diasAlerta = configDao.obtener().diasAlertaSaldo();
        return saldo().stream().filter(s -> s.dias() >= diasAlerta).toList();
    }

    @Override
    @Transactional
    public BigDecimal registrarAutoconsumo(int idProducto, BigDecimal kilos, int idActor) {
        Producto p = producto(idProducto);
        BigDecimal k = Validar.montoPositivo(kilos, "Ingrese los kilos");
        BigDecimal valor = salida(p, k, "AUTOCONSUMO", "Consumo de la casa", idActor);
        auditoria.registrar(idActor, "AUTOCONSUMO", p.nombre() + " · " + k + " kg · S/ " + valor);
        return valor;
    }

    @Override
    @Transactional
    public BigDecimal registrarAjuste(int idProducto, BigDecimal kilos, String tipo, String motivo, int idActor) {
        Producto p = producto(idProducto);
        if (!TIPOS_AJUSTE.contains(tipo)) {
            throw new NegocioException("Elija el tipo de ajuste");
        }
        if (StringUtils.isBlank(motivo)) {
            throw new NegocioException("Indique el motivo del ajuste");
        }
        if (kilos == null || kilos.signum() == 0) {
            throw new NegocioException("Ingrese los kilos del ajuste (negativo para restar)");
        }
        BigDecimal k = Dinero.r2(kilos);
        BigDecimal valor;
        if (k.signum() < 0) {
            valor = salida(p, k.abs(), tipo, motivo, idActor);
        } else {
            valor = entrada(p, k, tipo, motivo, idActor);
        }
        auditoria.registrar(idActor, "AJUSTE_SALDO", p.nombre() + " · " + k + " kg · " + tipo + " · " + motivo);
        return valor;
    }

    @Override
    public Map<Integer, BigDecimal> kilosEsperados() {
        Map<Integer, BigDecimal> kilos = loteDao.kilosPorProducto();
        Map<Integer, BigDecimal> out = new LinkedHashMap<>();
        for (Producto p : productoDao.listarActivos()) {
            out.put(p.id(), kilos.getOrDefault(p.id(), Dinero.CERO));
        }
        return out;
    }

    @Override
    @Transactional
    public BigDecimal registrarCierre(Map<Integer, BigDecimal> kilosReales, Map<Integer, String> motivos, int idActor) {
        Configuracion cfg = configDao.obtener();
        Map<Integer, BigDecimal> esperados = kilosEsperados();
        List<String> faltaMotivo = new ArrayList<>();
        for (Map.Entry<Integer, BigDecimal> e : kilosReales.entrySet()) {
            if (e.getValue() == null) {
                continue;
            }
            if (e.getValue().signum() < 0) {
                throw new NegocioException("Los kilos reales no pueden ser negativos");
            }
            BigDecimal esperado = esperados.getOrDefault(e.getKey(), Dinero.CERO);
            BigDecimal pct = Calculos.porcentajeDiferencia(Dinero.r2(e.getValue()), esperado);
            if (pct.compareTo(cfg.toleranciaConsumo()) > 0 && StringUtils.isBlank(motivos.get(e.getKey()))) {
                faltaMotivo.add(producto(e.getKey()).nombre() + ": diferencia de " + pct + "%");
            }
        }
        if (!faltaMotivo.isEmpty()) {
            throw new NegocioException(String.join(" · ", faltaMotivo) + ". Indique el motivo");
        }
        BigDecimal totalReal = BigDecimal.ZERO;
        int productos = 0;
        for (Map.Entry<Integer, BigDecimal> e : kilosReales.entrySet()) {
            if (e.getValue() == null) {
                continue;
            }
            Producto p = producto(e.getKey());
            BigDecimal real = Dinero.r2(e.getValue());
            BigDecimal dif = real.subtract(esperados.getOrDefault(p.id(), Dinero.CERO));
            String motivo = StringUtils.defaultIfBlank(motivos.get(p.id()), "Diferencia al cierre del puesto");
            if (dif.signum() < 0) {
                salida(p, dif.abs(), "CIERRE", motivo, idActor);
            } else if (dif.signum() > 0) {
                entrada(p, dif, "CIERRE", motivo, idActor);
            }
            totalReal = totalReal.add(real);
            productos++;
        }
        auditoria.registrar(idActor, "CIERRE_PUESTO", "Saldo para mañana: " + totalReal + " kg en " + productos + " productos");
        return totalReal;
    }

    /** Descuenta kilos por FIFO y registra el ajuste valorizado al costo de cada lote. */
    private BigDecimal salida(Producto p, BigDecimal kilos, String tipo, String motivo, int idActor) {
        BigDecimal disponible = loteDao.kilosDisponibles(p.id());
        if (kilos.compareTo(disponible) > 0) {
            throw new NegocioException("Solo hay " + disponible + " kg de " + p.nombre().toLowerCase() + ": el saldo no puede quedar negativo");
        }
        ResultadoFifo fifo = Calculos.consumirFifo(loteDao.disponiblesFifo(p.id()), kilos);
        BigDecimal valor = BigDecimal.ZERO;
        for (Consumo c : fifo.consumos()) {
            loteDao.descontar(c.idLote(), c.kilos());
            BigDecimal v = Dinero.r2(c.kilos().multiply(c.costoKg()));
            loteDao.insertarAjuste(c.idLote(), idActor, Fechas.ahora(), tipo, c.kilos().negate(), v, motivo);
            valor = valor.add(v);
        }
        return Dinero.r2(valor);
    }

    /** Suma kilos al lote más reciente (o crea uno) y registra el ajuste con valor negativo (no es pérdida). */
    private BigDecimal entrada(Producto p, BigDecimal kilos, String tipo, String motivo, int idActor) {
        BigDecimal costo = precioDao.ultimoCosto(p.id(), Fechas.hoy()).orElse(BigDecimal.ZERO);
        int idLote = loteDao.ultimoLote(p.id()).orElseGet(() -> loteDao.insertar(p.id(), null, Fechas.hoy(), Dinero.CERO, costo));
        loteDao.sumar(idLote, kilos);
        BigDecimal v = Dinero.r2(kilos.multiply(costo)).negate();
        loteDao.insertarAjuste(idLote, idActor, Fechas.ahora(), tipo, kilos, v, motivo);
        return v;
    }

    private Producto producto(int idProducto) {
        return productoDao.buscar(idProducto).orElseThrow(() -> new NegocioException("Producto no válido"));
    }
}
