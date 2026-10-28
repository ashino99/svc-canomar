package pe.canomar.svc.servicio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.PlatformTransactionManager;
import pe.canomar.svc.comun.Calculos.LoteKg;
import pe.canomar.svc.comun.ConfirmacionException;
import pe.canomar.svc.comun.NegocioException;
import pe.canomar.svc.dao.ClienteDao;
import pe.canomar.svc.dao.LoteDao;
import pe.canomar.svc.dao.PrecioDao;
import pe.canomar.svc.dao.ProductoDao;
import pe.canomar.svc.dao.VentaDao;
import pe.canomar.svc.modelo.Cliente;
import pe.canomar.svc.modelo.ItemVenta;
import pe.canomar.svc.modelo.NuevaVenta;
import pe.canomar.svc.modelo.Producto;
import pe.canomar.svc.modelo.SaldoCliente;
import pe.canomar.svc.modelo.VentaVista;
import pe.canomar.svc.servicio.impl.VentaServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas de la venta por kilo (RF17–RF21) con DAO simulados (Mockito). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VentaServiceImplTest {

    @Mock VentaDao ventaDao;
    @Mock ProductoDao productoDao;
    @Mock ClienteDao clienteDao;
    @Mock PrecioDao precioDao;
    @Mock LoteDao loteDao;
    @Mock CreditoService credito;
    @Mock AuditoriaService auditoria;
    @Mock PlatformTransactionManager tx;

    VentaServiceImpl servicio;

    private static BigDecimal d(String v) {
        return new BigDecimal(v);
    }

    @BeforeEach
    void preparar() {
        servicio = new VentaServiceImpl(ventaDao, productoDao, clienteDao, precioDao, loteDao, credito, auditoria, tx);
        when(productoDao.buscar(1)).thenReturn(Optional.of(new Producto(1, "Corvina", "Corvina", "PESCADO", "ACTIVO")));
        when(precioDao.precioDe(eq(1), any())).thenReturn(Optional.of(d("32.00")));
        when(precioDao.ultimoCosto(eq(1), any())).thenReturn(Optional.of(d("26.00")));
        when(loteDao.kilosDisponibles(1)).thenReturn(d("15.00"));
        when(loteDao.disponiblesFifo(1)).thenReturn(List.of(new LoteKg(10, d("15.00"), d("26.00"))));
        when(ventaDao.insertarVenta(any(), anyInt(), any())).thenReturn(7L);
        when(ventaDao.buscar(7L)).thenReturn(Optional.of(new VentaVista(7, "u", LocalDateTime.now(), null, "Público", "CONTADO", "YAPE",
                false, "VIGENTE", null, d("59.20"), "vendedor", null, false)));
        when(clienteDao.buscar(5)).thenReturn(Optional.of(new Cliente(5, "Cevichería El Muelle", "20601234561", null, d("1500.00"), 15, "ACTIVO")));
    }

    private NuevaVenta contado(String kilos, String precio) {
        return new NuevaVenta(null, null, "CONTADO", "YAPE", null, false,
                List.of(new ItemVenta(1, d(kilos), precio == null ? null : d(precio))));
    }

    private NuevaVenta credito(String kilos) {
        return new NuevaVenta(null, null, "CREDITO", null, 5, false, List.of(new ItemVenta(1, d(kilos), null)));
    }

    @Test
    @DisplayName("RF17/RF19: venta al contado usa el precio del día, descuenta el saldo y guarda el costo")
    void ventaContado() {
        VentaVista v = servicio.registrar(contado("1.85", null), 2, false, false);
        assertEquals(7L, v.id());
        verify(loteDao).descontar(10, d("1.85"));
        verify(ventaDao).insertarDetalle(7L, 1, d("1.85"), d("32.00"), d("26.00"));
    }

    @Test
    @DisplayName("RF17 alterno: si los kilos superan el saldo, pide confirmación")
    void saldoInsuficiente() {
        when(loteDao.kilosDisponibles(1)).thenReturn(d("1.80"));
        assertThrows(ConfirmacionException.class, () -> servicio.registrar(contado("2.00", null), 2, false, false));
        verify(ventaDao, never()).insertarVenta(any(), anyInt(), any());
    }

    @Test
    @DisplayName("RF19 alterno: sin precio del día, se exige el precio")
    void sinPrecio() {
        when(precioDao.precioDe(eq(1), any())).thenReturn(Optional.empty());
        NegocioException e = assertThrows(NegocioException.class, () -> servicio.registrar(contado("1.00", null), 2, false, false));
        assertTrue(e.getMessage().contains("precio de hoy"));
    }

    @Test
    @DisplayName("RF18: venta al crédito dentro del límite")
    void creditoDentroDelLimite() {
        when(credito.saldo(5)).thenReturn(new SaldoCliente(5, "Cevichería El Muelle", "20601234561", d("1500.00"), 15, d("816.00"), d("0.00"), null));
        servicio.registrar(credito("5.40"), 2, false, false);
        verify(ventaDao).insertarVenta(any(), eq(2), any());
    }

    @Test
    @DisplayName("RF18 alterno: el vendedor no puede superar el límite de crédito")
    void creditoSobreLimiteVendedor() {
        when(credito.saldo(5)).thenReturn(new SaldoCliente(5, "Cevichería El Muelle", "20601234561", d("1500.00"), 15, d("1450.00"), d("0.00"), null));
        NegocioException e = assertThrows(NegocioException.class, () -> servicio.registrar(credito("5.40"), 2, false, false));
        assertTrue(e.getMessage().contains("autorización del administrador"));
    }

    @Test
    @DisplayName("RF18 alterno: el administrador puede autorizar, previa confirmación")
    void creditoSobreLimiteAdministrador() {
        when(credito.saldo(5)).thenReturn(new SaldoCliente(5, "Cevichería El Muelle", "20601234561", d("1500.00"), 15, d("1450.00"), d("0.00"), null));
        assertThrows(ConfirmacionException.class, () -> servicio.registrar(credito("5.40"), 1, true, false));
        servicio.registrar(credito("5.40"), 1, true, true);
        verify(ventaDao).insertarVenta(any(), eq(1), any());
    }

    @Test
    @DisplayName("RF18 alterno: cliente sin crédito habilitado solo compra al contado")
    void clienteSinCredito() {
        when(clienteDao.buscar(5)).thenReturn(Optional.of(new Cliente(5, "Público frecuente", null, null, d("0.00"), 7, "ACTIVO")));
        assertThrows(NegocioException.class, () -> servicio.registrar(credito("1.00"), 2, false, false));
    }

    @Test
    @DisplayName("RF21 alterno: el precio de remate debe ser menor al del día")
    void remateConPrecioAlto() {
        NuevaVenta remate = new NuevaVenta(null, null, "CONTADO", "EFECTIVO", null, true, List.of(new ItemVenta(1, d("8.00"), d("35.00"))));
        assertThrows(NegocioException.class, () -> servicio.registrar(remate, 2, false, false));
    }

    @Test
    @DisplayName("RF25: una venta ya sincronizada se ignora (no se duplica)")
    void sincronizarDuplicada() {
        when(ventaDao.existeUuid("abc")).thenReturn(true);
        NuevaVenta v = new NuevaVenta("abc", LocalDateTime.now(), "CONTADO", "EFECTIVO", null, false, List.of(new ItemVenta(1, d("1.00"), null)));
        var r = servicio.sincronizar(List.of(v), 2);
        assertEquals("DUPLICADA", r.get(0).estado());
        verify(ventaDao, never()).insertarDetalle(anyLong(), anyInt(), any(), any(), any());
    }
}
