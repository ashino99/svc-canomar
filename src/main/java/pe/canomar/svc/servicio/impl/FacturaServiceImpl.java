package pe.canomar.svc.servicio.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.canomar.svc.comun.ConfirmacionException;
import pe.canomar.svc.comun.Dinero;
import pe.canomar.svc.comun.Fechas;
import pe.canomar.svc.comun.NegocioException;
import pe.canomar.svc.dao.ClienteDao;
import pe.canomar.svc.dao.FacturaDao;
import pe.canomar.svc.dao.VentaDao;
import pe.canomar.svc.modelo.Cliente;
import pe.canomar.svc.modelo.FacturaVista;
import pe.canomar.svc.modelo.PendienteFactura;
import pe.canomar.svc.modelo.VentaVista;
import pe.canomar.svc.servicio.AuditoriaService;
import pe.canomar.svc.servicio.FacturaService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FacturaServiceImpl implements FacturaService {

    private final FacturaDao facturaDao;
    private final ClienteDao clienteDao;
    private final VentaDao ventaDao;
    private final AuditoriaService auditoria;

    public FacturaServiceImpl(FacturaDao facturaDao, ClienteDao clienteDao, VentaDao ventaDao, AuditoriaService auditoria) {
        this.facturaDao = facturaDao;
        this.clienteDao = clienteDao;
        this.ventaDao = ventaDao;
        this.auditoria = auditoria;
    }

    @Override
    @Transactional
    public int registrar(int idCliente, String serie, int numero, LocalDate fechaEmision, BigDecimal importe, boolean detraccion, int idActor) {
        Cliente c = clienteDao.buscar(idCliente).orElseThrow(() -> new NegocioException("Elija el cliente"));
        if (StringUtils.isBlank(c.ruc())) {
            throw new NegocioException("El cliente no tiene RUC; no se puede registrar factura");
        }
        String s = StringUtils.upperCase(StringUtils.trimToEmpty(serie));
        if (!s.matches("[EF][A-Z0-9]{3}")) {
            throw new NegocioException("La serie debe tener 4 caracteres y empezar con E o F (por ejemplo E001)");
        }
        if (numero <= 0) {
            throw new NegocioException("Ingrese el número de la factura");
        }
        if (fechaEmision == null || fechaEmision.isAfter(Fechas.hoy())) {
            throw new NegocioException("La fecha de emisión no puede ser futura");
        }
        BigDecimal imp = Validar.montoPositivo(importe, "Ingrese el importe de la factura");
        if (facturaDao.existe(s, numero)) {
            throw new NegocioException("La factura " + s + "-" + String.format("%06d", numero) + " ya está registrada");
        }
        int id = facturaDao.insertar(idCliente, s, numero, fechaEmision, imp, detraccion);
        auditoria.registrar(idActor, "REGISTRAR_FACTURA", s + "-" + String.format("%06d", numero) + " · " + c.nombreComercial() + " · S/ " + imp);
        return id;
    }

    @Override
    @Transactional
    public BigDecimal asociar(int idFactura, List<Long> idsVenta, boolean confirmado, int idActor) {
        FacturaVista f = buscar(idFactura);
        if (idsVenta == null || idsVenta.isEmpty()) {
            throw new NegocioException("Seleccione las ventas que sustenta la factura");
        }
        Map<Long, VentaVista> libres = ventaDao.sinFacturaCliente(f.idCliente()).stream()
                .collect(Collectors.toMap(VentaVista::id, Function.identity()));
        BigDecimal suma = BigDecimal.ZERO;
        for (Long id : idsVenta) {
            VentaVista v = libres.get(id);
            if (v == null) {
                throw new NegocioException("La venta " + String.format("%04d", id) + " ya está asociada a otra factura o no es de este cliente");
            }
            suma = suma.add(v.total());
        }
        BigDecimal dif = Dinero.r2(f.diferencia().subtract(suma));
        if (dif.signum() != 0 && !confirmado) {
            throw new ConfirmacionException("La suma de las ventas (S/ " + Dinero.r2(suma) + ") difiere del importe pendiente de la factura en S/ " + dif + ". ¿Confirmar?");
        }
        ventaDao.asignarFactura(idsVenta, idFactura);
        auditoria.registrar(idActor, "ASOCIAR_VENTAS", f.comprobante() + " · " + idsVenta.size() + " ventas · diferencia S/ " + dif);
        return dif;
    }

    @Override
    public List<FacturaVista> listar() {
        return facturaDao.listar(50);
    }

    @Override
    public FacturaVista buscar(int idFactura) {
        return facturaDao.buscar(idFactura).orElseThrow(() -> new NegocioException("La factura no existe"));
    }

    @Override
    public List<VentaVista> ventasSinFactura(int idCliente) {
        return ventaDao.sinFacturaCliente(idCliente);
    }

    @Override
    public List<PendienteFactura> pendientes(YearMonth mes) {
        return ventaDao.pendientesFacturar(mes.atDay(1), mes.atEndOfMonth());
    }
}
