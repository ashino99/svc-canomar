package pe.canomar.svc.servicio.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.canomar.svc.comun.Calculos;
import pe.canomar.svc.comun.ConfirmacionException;
import pe.canomar.svc.comun.Dinero;
import pe.canomar.svc.comun.Fechas;
import pe.canomar.svc.comun.NegocioException;
import pe.canomar.svc.dao.LoteDao;
import pe.canomar.svc.dao.PrecioDao;
import pe.canomar.svc.modelo.PrecioVista;
import pe.canomar.svc.servicio.AuditoriaService;
import pe.canomar.svc.servicio.PrecioService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PrecioServiceImpl implements PrecioService {

    private final PrecioDao precioDao;
    private final LoteDao loteDao;
    private final AuditoriaService auditoria;

    public PrecioServiceImpl(PrecioDao precioDao, LoteDao loteDao, AuditoriaService auditoria) {
        this.precioDao = precioDao;
        this.loteDao = loteDao;
        this.auditoria = auditoria;
    }

    @Override
    public List<PrecioVista> preciosDeHoy() {
        return precioDao.preciosDe(Fechas.hoy());
    }

    @Override
    @Transactional
    public int guardarPrecios(Map<Integer, BigDecimal> precios, boolean confirmado, int idActor) {
        LocalDate hoy = Fechas.hoy();
        List<PrecioVista> actuales = precioDao.preciosDe(hoy);
        List<String> bajoCosto = new ArrayList<>();
        int guardados = 0;
        for (PrecioVista p : actuales) {
            BigDecimal precio = precios.get(p.idProducto());
            if (precio == null) {
                continue;
            }
            if (!Dinero.positivo(precio)) {
                throw new NegocioException(p.producto() + ": el precio debe ser mayor que cero");
            }
            if (p.costoKg() != null && Dinero.r2(precio).compareTo(p.costoKg()) < 0) {
                bajoCosto.add(p.producto() + ": S/ " + Dinero.r2(precio) + " está por debajo del costo S/ " + p.costoKg());
            }
        }
        if (!bajoCosto.isEmpty() && !confirmado) {
            throw new ConfirmacionException(String.join(" · ", bajoCosto) + ". ¿Confirmar los precios?");
        }
        for (PrecioVista p : actuales) {
            BigDecimal precio = precios.get(p.idProducto());
            if (precio != null) {
                precioDao.guardarPrecio(p.idProducto(), hoy, Dinero.r2(precio), Dinero.nz(p.costoKg()));
                guardados++;
            }
        }
        auditoria.registrar(idActor, "PRECIOS_DEL_DIA", guardados + " productos");
        return guardados;
    }

    @Override
    @Transactional
    public int usarPreciosDeAyer(int idActor) {
        LocalDate hoy = Fechas.hoy();
        int n = 0;
        for (PrecioVista p : precioDao.preciosDe(hoy)) {
            if (p.precioHoy() == null && p.precioAnterior() != null) {
                precioDao.guardarPrecio(p.idProducto(), hoy, p.precioAnterior(), Dinero.nz(p.costoKg()));
                n++;
            }
        }
        auditoria.registrar(idActor, "PRECIOS_DEL_DIA", "Se mantienen los precios anteriores de " + n + " productos");
        return n;
    }

    @Override
    @Transactional
    public BigDecimal recalcularCosto(int idProducto) {
        LocalDate hoy = Fechas.hoy();
        BigDecimal costo = Calculos.costoPromedio(loteDao.disponiblesFifo(idProducto));
        if (costo.signum() == 0) {
            return costo;
        }
        BigDecimal precioDefecto = precioDao.precioDe(idProducto, hoy)
                .or(() -> precioDao.ultimoPrecio(idProducto, hoy))
                .orElse(costo);
        precioDao.guardarCosto(idProducto, hoy, costo, precioDefecto);
        return costo;
    }
}
