package pe.canomar.svc.servicio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import pe.canomar.svc.comun.NegocioException;
import pe.canomar.svc.dao.AbonoDao;
import pe.canomar.svc.dao.CompraDao;
import pe.canomar.svc.dao.ConfiguracionDao;
import pe.canomar.svc.dao.CuadreDao;
import pe.canomar.svc.dao.GastoDao;
import pe.canomar.svc.dao.LoteDao;
import pe.canomar.svc.dao.VentaDao;
import pe.canomar.svc.modelo.Configuracion;
import pe.canomar.svc.modelo.Ganancia;
import pe.canomar.svc.modelo.Movimientos;
import pe.canomar.svc.servicio.impl.CajaServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/** Pruebas de gastos, cuadre y ganancia del día (RF38–RF40). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CajaServiceImplTest {

    @Mock GastoDao gastoDao;
    @Mock CuadreDao cuadreDao;
    @Mock VentaDao ventaDao;
    @Mock AbonoDao abonoDao;
    @Mock CompraDao compraDao;
    @Mock LoteDao loteDao;
    @Mock ConfiguracionDao configDao;
    @Mock AuditoriaService auditoria;

    CajaServiceImpl servicio;
    final LocalDate dia = LocalDate.of(2026, 10, 21);

    private static BigDecimal d(String v) {
        return new BigDecimal(v);
    }

    @BeforeEach
    void preparar() {
        servicio = new CajaServiceImpl(gastoDao, cuadreDao, ventaDao, abonoDao, compraDao, loteDao, configDao, auditoria);
        when(configDao.obtener()).thenReturn(new Configuracion(3, d("5.00"), d("5.00")));
        when(ventaDao.ventasContado(dia, true)).thenReturn(d("1032.40"));
        when(ventaDao.ventasContado(dia, false)).thenReturn(d("986.30"));
        when(abonoDao.abonosDelDia(eq(dia), anyBoolean())).thenReturn(d("0.00"));
        when(gastoDao.totalDelDia(dia, true)).thenReturn(d("47.00"));
        when(gastoDao.totalDelDia(dia, false)).thenReturn(d("0.00"));
        when(compraDao.pagosDelDia(eq(dia), anyBoolean())).thenReturn(d("0.00"));
        when(cuadreDao.buscar(dia)).thenReturn(Optional.empty());
        when(ventaDao.cantidadVentas(dia, dia)).thenReturn(65);
        when(ventaDao.totalVentas(dia, dia)).thenReturn(d("2603.20"));
        when(ventaDao.costoVendido(dia, dia)).thenReturn(d("1912.60"));
        when(loteDao.perdidas(dia, dia)).thenReturn(d("27.60"));
        when(gastoDao.total(dia, dia)).thenReturn(d("47.00"));
    }

    @Test
    @DisplayName("RF39: efectivo esperado = contado + abonos − gastos − pagos")
    void esperado() {
        Movimientos m = servicio.movimientos(dia);
        assertEquals(d("985.40"), m.efectivoEsperado());
        assertEquals(d("986.30"), m.digitalEsperado());
    }

    @Test
    @DisplayName("RF39 alterno: diferencia mayor a la tolerancia sin motivo")
    void diferenciaSinMotivo() {
        assertThrows(NegocioException.class, () -> servicio.realizarCuadre(dia, d("945.40"), d("986.30"), null, 1));
    }

    @Test
    @DisplayName("RF40: ganancia del día")
    void ganancia() {
        Ganancia g = servicio.ganancia(dia);
        assertEquals(d("616.00"), g.ganancia());
        assertEquals(true, g.provisional());
    }

    @Test
    @DisplayName("RF38 alterno: un gasto 'Otro' necesita descripción")
    void gastoOtroSinDescripcion() {
        assertThrows(NegocioException.class, () -> servicio.registrarGasto("OTRO", " ", d("10.00"), "EFECTIVO", 2));
    }

    @Test
    @DisplayName("RF39 alterno: no se cuadra dos veces el mismo día")
    void cuadreRepetido() {
        when(cuadreDao.buscar(dia)).thenReturn(Optional.of(new pe.canomar.svc.modelo.CuadreVista(1, dia, d("1"), d("1"), d("1"), d("1"), null, d("1"), "cuentas")));
        assertThrows(NegocioException.class, () -> servicio.realizarCuadre(dia, d("985.40"), d("986.30"), null, 1));
    }

    @Test
    @DisplayName("RF39: dentro de la tolerancia se guarda sin motivo")
    void cuadreOk() {
        when(cuadreDao.buscar(dia)).thenReturn(Optional.empty(), Optional.empty(),
                Optional.of(new pe.canomar.svc.modelo.CuadreVista(1, dia, d("985.40"), d("982.40"), d("986.30"), d("986.30"), null, d("616.00"), "cuentas")));
        var c = servicio.realizarCuadre(dia, d("982.40"), d("986.30"), null, 1);
        assertEquals(d("-3.00"), c.diferenciaEfectivo());
        org.mockito.Mockito.verify(cuadreDao).insertar(eq(1), eq(dia), eq(d("985.40")), eq(d("982.40")), eq(d("986.30")), eq(d("986.30")), any(), eq(d("616.00")));
    }
}
