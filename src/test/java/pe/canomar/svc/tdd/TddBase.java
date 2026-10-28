package pe.canomar.svc.tdd;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import pe.canomar.svc.comun.Calculos.LoteKg;
import pe.canomar.svc.comun.Fechas;
import pe.canomar.svc.dao.*;
import pe.canomar.svc.modelo.*;
import pe.canomar.svc.seguridad.ManejadorAcceso;
import pe.canomar.svc.servicio.AuditoriaService;
import pe.canomar.svc.servicio.CreditoService;
import pe.canomar.svc.servicio.PrecioService;
import pe.canomar.svc.servicio.impl.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.*;

/**
 * Base de las pruebas TDD (una clase de prueba por RF, un método por caso de prueba).
 * <p>
 * Cada DAO es un <b>DAO falso</b> creado con Mockito: no hay base de datos ni Spring.
 * JUnit crea una instancia nueva por cada test, así que cada caso empieza con DAO limpios.
 * Las reglas que se prueban viven en las clases Service; la pantalla no se usa.
 */
abstract class TddBase {

    // ---------- DAO falsos ----------
    protected final UsuarioDao usuarioDao = mock(UsuarioDao.class);
    protected final ProductoDao productoDao = mock(ProductoDao.class);
    protected final ClienteDao clienteDao = mock(ClienteDao.class);
    protected final ProveedorDao proveedorDao = mock(ProveedorDao.class);
    protected final CompraDao compraDao = mock(CompraDao.class);
    protected final LoteDao loteDao = mock(LoteDao.class);
    protected final PrecioDao precioDao = mock(PrecioDao.class);
    protected final VentaDao ventaDao = mock(VentaDao.class);
    protected final AbonoDao abonoDao = mock(AbonoDao.class);
    protected final CompromisoDao compromisoDao = mock(CompromisoDao.class);
    protected final GastoDao gastoDao = mock(GastoDao.class);
    protected final CuadreDao cuadreDao = mock(CuadreDao.class);
    protected final FacturaDao facturaDao = mock(FacturaDao.class);
    protected final ConfiguracionDao configDao = mock(ConfiguracionDao.class);
    protected final AuditoriaDao auditoriaDao = mock(AuditoriaDao.class);
    protected final RespaldoDao respaldoDao = mock(RespaldoDao.class);

    // ---------- servicios vecinos simulados ----------
    protected final AuditoriaService auditoria = mock(AuditoriaService.class);
    protected final CreditoService credito = mock(CreditoService.class);
    protected final PrecioService precioService = mock(PrecioService.class);
    protected final PlatformTransactionManager txManager = mock(PlatformTransactionManager.class);
    protected final PasswordEncoder encoder = new BCryptPasswordEncoder(4);

    // ---------- servicios reales que se prueban ----------
    protected UsuarioServiceImpl usuarioService() {
        return new UsuarioServiceImpl(usuarioDao, encoder, auditoria);
    }

    protected ManejadorAcceso manejadorAcceso() {
        return new ManejadorAcceso(usuarioDao, auditoriaDao);
    }

    protected ProductoServiceImpl productoService() {
        return new ProductoServiceImpl(productoDao, loteDao, auditoria);
    }

    protected ClienteServiceImpl clienteService() {
        return new ClienteServiceImpl(clienteDao, credito, auditoria);
    }

    protected CompraServiceImpl compraService() {
        return new CompraServiceImpl(compraDao, proveedorDao, productoDao, loteDao, precioService, auditoria);
    }

    protected PrecioServiceImpl precioServiceReal() {
        return new PrecioServiceImpl(precioDao, loteDao, auditoria);
    }

    protected VentaServiceImpl ventaService() {
        return new VentaServiceImpl(ventaDao, productoDao, clienteDao, precioDao, loteDao, credito, auditoria, txManager);
    }

    protected SaldoServiceImpl saldoService() {
        return new SaldoServiceImpl(loteDao, productoDao, precioDao, configDao, auditoria);
    }

    protected CreditoServiceImpl creditoService() {
        return new CreditoServiceImpl(ventaDao, abonoDao, clienteDao, compromisoDao, auditoria);
    }

    protected CajaServiceImpl cajaService() {
        return new CajaServiceImpl(gastoDao, cuadreDao, ventaDao, abonoDao, compraDao, loteDao, configDao, auditoria);
    }

    protected FacturaServiceImpl facturaService() {
        return new FacturaServiceImpl(facturaDao, clienteDao, ventaDao, auditoria);
    }

    protected ReporteServiceImpl reporteService() {
        return new ReporteServiceImpl(ventaDao, compraDao, gastoDao, loteDao, facturaDao, cuadreDao);
    }

    protected RespaldoServiceImpl respaldoService() {
        return new RespaldoServiceImpl(respaldoDao, auditoria);
    }

    // ---------- datos de prueba (los mismos del negocio) ----------
    protected static BigDecimal d(String valor) {
        return new BigDecimal(valor);
    }

    protected static Configuracion config() {
        return new Configuracion(3, d("5.00"), d("5.00"));
    }

    protected static Usuario usuario(int id, String login, String rol, String estado, int intentos) {
        return new Usuario(id, "Usuario " + login, login, "$2a$04$hash", rol, estado, intentos, null, Fechas.ahora());
    }

    protected Usuario usuarioConClave(int id, String login, String clave) {
        return new Usuario(id, "Usuario " + login, login, encoder.encode(clave), "VENDEDOR", "ACTIVO", 0, null, Fechas.ahora());
    }

    protected static Producto producto(int id, String nombre) {
        return new Producto(id, nombre, nombre, "PESCADO", "ACTIVO");
    }

    protected static Cliente cliente(int id, String nombre, String limite, int plazo) {
        return new Cliente(id, nombre, "2060123456" + id, "98765432" + id, d(limite), plazo, "ACTIVO");
    }

    protected static SaldoCliente saldoCliente(int id, String saldo, String limite, String vencido) {
        return new SaldoCliente(id, "Cliente " + id, null, d(limite), 7, d(saldo), d(vencido), Fechas.hoy().minusDays(10));
    }

    protected static DeudaProveedor deudaProveedor(int id, String nombre, String compras, String pagos) {
        return new DeudaProveedor(id, nombre, "PRINCIPAL", d(compras), d(pagos), Fechas.hoy().minusDays(3));
    }

    protected static PrecioVista precioVista(int id, String nombre, String costo, String precioHoy, String precioAyer) {
        return new PrecioVista(id, nombre, nombre, d(costo), precioHoy == null ? null : d(precioHoy),
                precioAyer == null ? null : d(precioAyer), d("10.00"));
    }

    protected static LoteVista lote(int idLote, int idProducto, String producto, int diasAtras, String kilos, String costo) {
        return new LoteVista(idLote, idProducto, producto, Fechas.hoy().minusDays(diasAtras), d(kilos), d(costo));
    }

    protected static VentaVista venta(long id, Integer idCliente, String tipo, String total, String estado, Integer idFactura) {
        return new VentaVista(id, "uuid-" + id, Fechas.ahora(), idCliente, idCliente == null ? "Público" : "Cliente " + idCliente, tipo,
                "CONTADO".equals(tipo) ? "EFECTIVO" : null, false, estado, null, d(total), "vendedor", idFactura, false);
    }

    protected static CuadreVista cuadreVista(LocalDate fecha) {
        return new CuadreVista(1, fecha, d("0.00"), d("0.00"), d("0.00"), d("0.00"), null, d("0.00"), "cuentas");
    }

    // ---------- ventas ----------
    protected static ItemVenta item(int idProducto, String kilos) {
        return new ItemVenta(idProducto, d(kilos), null);
    }

    protected static ItemVenta item(int idProducto, String kilos, String precio) {
        return new ItemVenta(idProducto, d(kilos), d(precio));
    }

    protected static NuevaVenta contado(String medio, ItemVenta... items) {
        return new NuevaVenta(null, null, "CONTADO", medio, null, false, Arrays.asList(items));
    }

    protected static NuevaVenta ventaCredito(int idCliente, ItemVenta... items) {
        return new NuevaVenta(null, null, "CREDITO", null, idCliente, false, Arrays.asList(items));
    }

    protected static NuevaVenta remate(Integer idCliente, ItemVenta... items) {
        return new NuevaVenta(null, null, idCliente == null ? "CONTADO" : "CREDITO", idCliente == null ? "EFECTIVO" : null,
                idCliente, true, Arrays.asList(items));
    }

    /** Corvina (2): precio de hoy S/ 32.00, costo S/ 26.00 y 15 kg en el lote 10. */
    protected void prepararCorvina() {
        when(productoDao.buscar(2)).thenReturn(Optional.of(producto(2, "Corvina")));
        when(precioDao.precioDe(eq(2), any())).thenReturn(Optional.of(d("32.00")));
        when(precioDao.ultimoCosto(eq(2), any())).thenReturn(Optional.of(d("26.00")));
        when(loteDao.kilosDisponibles(2)).thenReturn(d("15.00"));
        when(loteDao.disponiblesFifo(2)).thenReturn(List.of(new LoteKg(10, d("15.00"), d("26.00"))));
        prepararInsercionVenta();
    }

    /** Bonito (1): precio de hoy S/ 9.00, costo S/ 6.50 y 8 kg antiguos en el lote 1. */
    protected void prepararBonito() {
        when(productoDao.buscar(1)).thenReturn(Optional.of(producto(1, "Bonito")));
        when(precioDao.precioDe(eq(1), any())).thenReturn(Optional.of(d("9.00")));
        when(precioDao.ultimoCosto(eq(1), any())).thenReturn(Optional.of(d("6.50")));
        when(loteDao.kilosDisponibles(1)).thenReturn(d("8.00"));
        when(loteDao.disponiblesFifo(1)).thenReturn(List.of(new LoteKg(1, d("8.00"), d("6.50"))));
        prepararInsercionVenta();
    }

    private void prepararInsercionVenta() {
        when(ventaDao.insertarVenta(any(), anyInt(), any())).thenReturn(7L);
        when(ventaDao.buscar(7L)).thenReturn(Optional.of(venta(7, null, "CONTADO", "0.00", "VIGENTE", null)));
    }

    // ---------- créditos ----------
    /** Restaurante Sabor Norteño (2): venta 1 de S/ 640 hace 20 días y venta 2 de S/ 732 hace 10 días. */
    protected void prepararDeudaSaborNorteno() {
        when(clienteDao.buscar(2)).thenReturn(Optional.of(cliente(2, "Restaurante Sabor Norteño", "1500.00", 7)));
        when(ventaDao.pendientesCliente(2)).thenReturn(List.of(
                new PendienteVenta(1L, Fechas.ahora().minusDays(20), d("640.00"), d("0.00")),
                new PendienteVenta(2L, Fechas.ahora().minusDays(10), d("732.00"), d("0.00"))));
    }

    // ---------- saldo y caja ----------
    protected void prepararCierre(int idProducto, String nombre, String kilosEsperados) {
        when(configDao.obtener()).thenReturn(config());
        when(productoDao.listarActivos()).thenReturn(List.of(producto(idProducto, nombre)));
        when(productoDao.buscar(idProducto)).thenReturn(Optional.of(producto(idProducto, nombre)));
        when(loteDao.kilosPorProducto()).thenReturn(Map.of(idProducto, d(kilosEsperados)));
        when(loteDao.kilosDisponibles(idProducto)).thenReturn(d(kilosEsperados));
        when(loteDao.disponiblesFifo(idProducto)).thenReturn(List.of(new LoteKg(2, d(kilosEsperados), d("5.00"))));
    }

    protected void prepararMovimientos(LocalDate f, String ventasEf, String ventasDig, String abonosEf, String abonosDig,
                                       String gastosEf, String gastosDig) {
        when(configDao.obtener()).thenReturn(config());
        when(ventaDao.ventasContado(f, true)).thenReturn(d(ventasEf));
        when(ventaDao.ventasContado(f, false)).thenReturn(d(ventasDig));
        when(abonoDao.abonosDelDia(f, true)).thenReturn(d(abonosEf));
        when(abonoDao.abonosDelDia(f, false)).thenReturn(d(abonosDig));
        when(gastoDao.totalDelDia(f, true)).thenReturn(d(gastosEf));
        when(gastoDao.totalDelDia(f, false)).thenReturn(d(gastosDig));
        when(compraDao.pagosDelDia(f, true)).thenReturn(d("0.00"));
        when(compraDao.pagosDelDia(f, false)).thenReturn(d("0.00"));
    }

    // ---------- facturas ----------
    protected void prepararFactura(int idFactura, int idCliente, String importe) {
        when(facturaDao.buscar(idFactura)).thenReturn(Optional.of(new FacturaVista(idFactura, idCliente, "Cliente " + idCliente, "E001", 154,
                Fechas.hoy(), d(importe), false, d("0.00"))));
    }
}
