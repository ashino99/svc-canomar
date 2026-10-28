package pe.canomar.svc.servicio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import pe.canomar.svc.comun.Fechas;
import pe.canomar.svc.comun.NegocioException;
import pe.canomar.svc.dao.AbonoDao;
import pe.canomar.svc.dao.ClienteDao;
import pe.canomar.svc.dao.CompromisoDao;
import pe.canomar.svc.dao.VentaDao;
import pe.canomar.svc.modelo.Antiguedad;
import pe.canomar.svc.modelo.Cliente;
import pe.canomar.svc.modelo.PendienteVenta;
import pe.canomar.svc.modelo.SaldoCliente;
import pe.canomar.svc.servicio.impl.CreditoServiceImpl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas de créditos y cobranza (RF31–RF35). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CreditoServiceImplTest {

    @Mock VentaDao ventaDao;
    @Mock AbonoDao abonoDao;
    @Mock ClienteDao clienteDao;
    @Mock CompromisoDao compromisoDao;
    @Mock AuditoriaService auditoria;

    CreditoServiceImpl servicio;

    private static BigDecimal d(String v) {
        return new BigDecimal(v);
    }

    @BeforeEach
    void preparar() {
        servicio = new CreditoServiceImpl(ventaDao, abonoDao, clienteDao, compromisoDao, auditoria);
        when(clienteDao.buscar(2)).thenReturn(Optional.of(new Cliente(2, "Restaurante Sabor Norteño", "20601234571", null, d("1500.00"), 7, "ACTIVO")));
        when(ventaDao.pendientesCliente(2)).thenReturn(List.of(
                new PendienteVenta(1, Fechas.ahora().minusDays(20), d("640.00"), d("0.00")),
                new PendienteVenta(2, Fechas.ahora().minusDays(3), d("732.00"), d("0.00"))));
        when(abonoDao.insertar(eq(2), anyInt(), any(), any(), any())).thenReturn(9);
    }

    @Test
    @DisplayName("RF32: saldo y deuda vencida según el plazo del cliente")
    void saldo() {
        SaldoCliente s = servicio.saldo(2);
        assertEquals(d("1372.00"), s.saldo());
        assertEquals(d("640.00"), s.vencido());
        assertEquals(d("128.00"), s.disponible());
    }

    @Test
    @DisplayName("RF31: el abono se aplica primero a la venta más antigua")
    void abonoFifo() {
        BigDecimal nuevo = servicio.registrarAbono(2, d("700.00"), "YAPE", 1);
        assertEquals(d("672.00"), nuevo);
        verify(abonoDao).aplicar(9, 1L, d("640.00"));
        verify(abonoDao).aplicar(9, 2L, d("60.00"));
    }

    @Test
    @DisplayName("RF31 alterno: el abono no puede superar el saldo")
    void abonoMayorAlSaldo() {
        assertThrows(NegocioException.class, () -> servicio.registrarAbono(2, d("2000.00"), "YAPE", 1));
    }

    @Test
    @DisplayName("RF31 alterno: si el abono cubre todo, los compromisos se cumplen")
    void abonoCompleto() {
        servicio.registrarAbono(2, d("1372.00"), "EFECTIVO", 1);
        verify(compromisoDao).marcarCumplidos(2);
    }

    @Test
    @DisplayName("RF33: la deuda se agrupa por tramos de antigüedad")
    void antiguedad() {
        List<Antiguedad> a = servicio.antiguedad(2);
        assertEquals(d("732.00"), a.get(0).monto());
        assertEquals(d("640.00"), a.get(2).monto());
    }

    @Test
    @DisplayName("RF35 alterno: la fecha del compromiso no puede ser pasada")
    void compromisoFechaPasada() {
        assertThrows(NegocioException.class, () -> servicio.registrarCompromiso(2, Fechas.hoy().minusDays(1), d("100.00"), 1));
    }
}
