package pe.canomar.svc.servicio.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.canomar.svc.comun.Calculos;
import pe.canomar.svc.comun.Dinero;
import pe.canomar.svc.comun.Fechas;
import pe.canomar.svc.comun.NegocioException;
import pe.canomar.svc.dao.AbonoDao;
import pe.canomar.svc.dao.CompraDao;
import pe.canomar.svc.dao.ConfiguracionDao;
import pe.canomar.svc.dao.CuadreDao;
import pe.canomar.svc.dao.GastoDao;
import pe.canomar.svc.dao.LoteDao;
import pe.canomar.svc.dao.VentaDao;
import pe.canomar.svc.modelo.CuadreVista;
import pe.canomar.svc.modelo.Ganancia;
import pe.canomar.svc.modelo.GastoVista;
import pe.canomar.svc.modelo.Movimientos;
import pe.canomar.svc.servicio.AuditoriaService;
import pe.canomar.svc.servicio.CajaService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class CajaServiceImpl implements CajaService {

    private static final Set<String> CONCEPTOS = Set.of("HIELO", "BOLSAS", "MOVILIDAD", "ESTIBA", "ESPACIO", "OTRO");

    private final GastoDao gastoDao;
    private final CuadreDao cuadreDao;
    private final VentaDao ventaDao;
    private final AbonoDao abonoDao;
    private final CompraDao compraDao;
    private final LoteDao loteDao;
    private final ConfiguracionDao configDao;
    private final AuditoriaService auditoria;

    public CajaServiceImpl(GastoDao gastoDao, CuadreDao cuadreDao, VentaDao ventaDao, AbonoDao abonoDao, CompraDao compraDao,
                           LoteDao loteDao, ConfiguracionDao configDao, AuditoriaService auditoria) {
        this.gastoDao = gastoDao;
        this.cuadreDao = cuadreDao;
        this.ventaDao = ventaDao;
        this.abonoDao = abonoDao;
        this.compraDao = compraDao;
        this.loteDao = loteDao;
        this.configDao = configDao;
        this.auditoria = auditoria;
    }

    @Override
    @Transactional
    public void registrarGasto(String concepto, String descripcion, BigDecimal monto, String medio, int idActor) {
        if (!CONCEPTOS.contains(concepto)) {
            throw new NegocioException("Elija el concepto del gasto");
        }
        if ("OTRO".equals(concepto) && StringUtils.isBlank(descripcion)) {
            throw new NegocioException("Describa el gasto");
        }
        BigDecimal m = Validar.montoPositivo(monto, "Ingrese un monto mayor que cero");
        Validar.medio(medio);
        gastoDao.insertar(idActor, Fechas.hoy(), concepto, StringUtils.left(StringUtils.trimToNull(descripcion), 100), m, medio);
        auditoria.registrar(idActor, "REGISTRAR_GASTO", concepto + " · S/ " + m + " · " + medio);
    }

    @Override
    public List<GastoVista> gastosDelDia(LocalDate fecha) {
        return gastoDao.listarPorFecha(fecha);
    }

    @Override
    public Movimientos movimientos(LocalDate f) {
        return new Movimientos(ventaDao.ventasContado(f, true), ventaDao.ventasContado(f, false),
                abonoDao.abonosDelDia(f, true), abonoDao.abonosDelDia(f, false),
                gastoDao.totalDelDia(f, true), gastoDao.totalDelDia(f, false),
                compraDao.pagosDelDia(f, true), compraDao.pagosDelDia(f, false));
    }

    @Override
    public Optional<CuadreVista> cuadre(LocalDate fecha) {
        return cuadreDao.buscar(fecha);
    }

    @Override
    @Transactional
    public CuadreVista realizarCuadre(LocalDate fecha, BigDecimal efectivoContado, BigDecimal digitalRevisado, String motivo, int idActor) {
        if (cuadreDao.buscar(fecha).isPresent()) {
            throw new NegocioException("El día " + fecha.format(Fechas.DIA) + " ya tiene cuadre");
        }
        if (efectivoContado == null || efectivoContado.signum() < 0 || digitalRevisado == null || digitalRevisado.signum() < 0) {
            throw new NegocioException("Ingrese el efectivo contado y lo revisado en Yape o transferencias");
        }
        Movimientos m = movimientos(fecha);
        BigDecimal difE = Calculos.diferencia(efectivoContado, m.efectivoEsperado());
        BigDecimal difD = Calculos.diferencia(digitalRevisado, m.digitalEsperado());
        BigDecimal tol = configDao.obtener().toleranciaCuadre();
        if ((difE.abs().compareTo(tol) > 0 || difD.abs().compareTo(tol) > 0) && StringUtils.isBlank(motivo)) {
            throw new NegocioException("Diferencia de S/ " + difE + " en efectivo y S/ " + difD + " en digital: indique el motivo");
        }
        BigDecimal ganancia = ganancia(fecha).ganancia();
        cuadreDao.insertar(idActor, fecha, m.efectivoEsperado(), Dinero.r2(efectivoContado), m.digitalEsperado(), Dinero.r2(digitalRevisado),
                StringUtils.left(StringUtils.trimToNull(motivo), 150), ganancia);
        auditoria.registrar(idActor, "CUADRE_CAJA", fecha.format(Fechas.DIA) + " · diferencia S/ " + difE + " · ganancia S/ " + ganancia);
        return cuadreDao.buscar(fecha).orElseThrow();
    }

    @Override
    public Ganancia ganancia(LocalDate f) {
        return new Ganancia(f, ventaDao.cantidadVentas(f, f), ventaDao.totalVentas(f, f), ventaDao.costoVendido(f, f),
                loteDao.perdidas(f, f), gastoDao.total(f, f), cuadreDao.buscar(f).isPresent());
    }

    @Override
    public List<CuadreVista> historial(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null || desde.isAfter(hasta)) {
            throw new NegocioException("Revise el rango de fechas");
        }
        return cuadreDao.listarEntre(desde, hasta);
    }
}
