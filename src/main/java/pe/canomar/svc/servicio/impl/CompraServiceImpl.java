package pe.canomar.svc.servicio.impl;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.canomar.svc.comun.Calculos;
import pe.canomar.svc.comun.Dinero;
import pe.canomar.svc.comun.Fechas;
import pe.canomar.svc.comun.NegocioException;
import pe.canomar.svc.dao.CompraDao;
import pe.canomar.svc.dao.LoteDao;
import pe.canomar.svc.dao.ProductoDao;
import pe.canomar.svc.dao.ProveedorDao;
import pe.canomar.svc.modelo.CompraVista;
import pe.canomar.svc.modelo.DeudaProveedor;
import pe.canomar.svc.modelo.ItemCompra;
import pe.canomar.svc.modelo.PagoProveedorVista;
import pe.canomar.svc.modelo.Producto;
import pe.canomar.svc.modelo.Proveedor;
import pe.canomar.svc.servicio.AuditoriaService;
import pe.canomar.svc.servicio.CompraService;
import pe.canomar.svc.servicio.PrecioService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class CompraServiceImpl implements CompraService {

    private static final Logger log = LoggerFactory.getLogger(CompraServiceImpl.class);

    private final CompraDao compraDao;
    private final ProveedorDao proveedorDao;
    private final ProductoDao productoDao;
    private final LoteDao loteDao;
    private final PrecioService precioService;
    private final AuditoriaService auditoria;

    public CompraServiceImpl(CompraDao compraDao, ProveedorDao proveedorDao, ProductoDao productoDao, LoteDao loteDao,
                             PrecioService precioService, AuditoriaService auditoria) {
        this.compraDao = compraDao;
        this.proveedorDao = proveedorDao;
        this.productoDao = productoDao;
        this.loteDao = loteDao;
        this.precioService = precioService;
        this.auditoria = auditoria;
    }

    @Override
    public List<Proveedor> proveedores() {
        return proveedorDao.listar();
    }

    @Override
    @Transactional
    public int registrarProveedor(String nombre, String ruc, String tipo, String telefono, int idActor) {
        String n = StringUtils.normalizeSpace(nombre);
        if (StringUtils.isBlank(n)) {
            throw new NegocioException("Ingrese el nombre del proveedor");
        }
        String r = StringUtils.trimToNull(ruc);
        if (r != null && !Calculos.rucValido(r)) {
            throw new NegocioException("El RUC debe tener 11 dígitos");
        }
        String t = "PRINCIPAL".equals(tipo) ? "PRINCIPAL" : "OCASIONAL";
        if (proveedorDao.existeNombre(n)) {
            throw new NegocioException("El proveedor ya está registrado");
        }
        int id = proveedorDao.insertar(new Proveedor(null, n, r, t, StringUtils.trimToNull(telefono)));
        auditoria.registrar(idActor, "REGISTRAR_PROVEEDOR", n + " · " + t);
        return id;
    }

    @Override
    @Transactional
    public int registrarCompra(int idProveedor, String tipoPago, List<ItemCompra> items, int idActor) {
        Proveedor prov = proveedorDao.buscar(idProveedor).orElseThrow(() -> new NegocioException("Elija el proveedor"));
        if (!"CONTADO".equals(tipoPago) && !"CREDITO".equals(tipoPago)) {
            throw new NegocioException("Indique si la compra fue al contado o a crédito");
        }
        if (items == null || items.isEmpty()) {
            throw new NegocioException("Agregue al menos un producto");
        }
        for (ItemCompra it : items) {
            Producto p = productoDao.buscar(it.idProducto()).orElseThrow(() -> new NegocioException("Producto no válido"));
            if (!p.activo()) {
                throw new NegocioException(p.nombre() + " está inactivo");
            }
            if (!Dinero.positivo(it.kilos()) || !Dinero.positivo(it.costoTotal())) {
                throw new NegocioException("Los kilos y el costo deben ser mayores que cero");
            }
        }
        LocalDate hoy = Fechas.hoy();
        int idCompra = compraDao.insertarCompra(idProveedor, idActor, hoy, tipoPago);
        Set<Integer> productos = new LinkedHashSet<>();
        BigDecimal kilos = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (ItemCompra it : items) {
            ItemCompra limpio = new ItemCompra(it.idProducto(), Dinero.r2(it.kilos()), Dinero.r2(it.costoTotal()));
            int idDetalle = compraDao.insertarDetalle(idCompra, limpio);
            loteDao.insertar(limpio.idProducto(), idDetalle, hoy, limpio.kilos(), Calculos.costoPorKilo(limpio.costoTotal(), limpio.kilos()));
            productos.add(limpio.idProducto());
            kilos = kilos.add(limpio.kilos());
            total = total.add(limpio.costoTotal());
        }
        productos.forEach(precioService::recalcularCosto);
        auditoria.registrar(idActor, "REGISTRAR_COMPRA", prov.nombre() + " · " + kilos + " kg · S/ " + total + " · " + tipoPago);
        log.info("Compra {} registrada: {} kg, S/ {}", idCompra, kilos, total);
        return idCompra;
    }

    @Override
    public List<CompraVista> comprasDeHoy() {
        return compraDao.listarPorFecha(Fechas.hoy());
    }

    @Override
    public List<DeudaProveedor> deudas() {
        return compraDao.deudas();
    }

    @Override
    @Transactional
    public BigDecimal registrarPago(int idProveedor, BigDecimal monto, String medio, int idActor) {
        BigDecimal m = Validar.montoPositivo(monto, "Ingrese un monto mayor que cero");
        Validar.medio(medio);
        DeudaProveedor d = compraDao.deudas().stream().filter(x -> x.idProveedor() == idProveedor).findFirst()
                .orElseThrow(() -> new NegocioException("Elija el proveedor"));
        if (m.compareTo(d.deuda()) > 0) {
            throw new NegocioException("El pago supera la deuda (S/ " + d.deuda() + ")");
        }
        compraDao.insertarPago(idProveedor, idActor, Fechas.hoy(), m, medio);
        auditoria.registrar(idActor, "PAGO_PROVEEDOR", d.proveedor() + " · S/ " + m + " · " + medio);
        return d.deuda().subtract(m);
    }

    @Override
    public List<PagoProveedorVista> pagosRecientes() {
        return compraDao.pagosRecientes(10);
    }
}
