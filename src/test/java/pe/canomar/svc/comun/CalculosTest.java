package pe.canomar.svc.comun;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.canomar.svc.comun.Calculos.Aplicacion;
import pe.canomar.svc.comun.Calculos.LoteKg;
import pe.canomar.svc.comun.Calculos.Pendiente;
import pe.canomar.svc.comun.Calculos.ResultadoFifo;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pruebas de las reglas de cálculo (escritas antes que los servicios: TDD). */
class CalculosTest {

    private static BigDecimal d(String v) {
        return new BigDecimal(v);
    }

    @Test
    @DisplayName("RF19: importe = kilos × precio, redondeado a 2 decimales")
    void importeRedondeado() {
        assertEquals(d("59.20"), Calculos.importe(d("1.85"), d("32.00")));
        assertEquals(d("21.15"), Calculos.importe(d("2.35"), d("9.00")));
        assertEquals(d("1.86"), Calculos.kilos(d("1.855")));
    }

    @Test
    @DisplayName("RF19: no se aceptan kilos en cero")
    void importeSinKilos() {
        assertThrows(IllegalArgumentException.class, () -> Calculos.importe(BigDecimal.ZERO, d("9.00")));
    }

    @Test
    @DisplayName("RF12: costo promedio ponderado del saldo anterior y la compra de hoy")
    void costoPromedio() {
        List<LoteKg> lotes = List.of(new LoteKg(1, d("8.00"), d("6.20")), new LoteKg(2, d("60.00"), d("6.50")));
        assertEquals(d("6.46"), Calculos.costoPromedio(lotes));
        assertEquals(d("0.00"), Calculos.costoPromedio(List.of()));
    }

    @Test
    @DisplayName("FIFO: se descuenta primero el lote más antiguo")
    void consumoFifo() {
        List<LoteKg> lotes = List.of(new LoteKg(1, d("3.00"), d("6.00")), new LoteKg(2, d("10.00"), d("7.00")));
        ResultadoFifo r = Calculos.consumirFifo(lotes, d("5.00"));
        assertEquals(2, r.consumos().size());
        assertEquals(d("3.00"), r.consumos().get(0).kilos());
        assertEquals(d("2.00"), r.consumos().get(1).kilos());
        assertEquals(d("0.00"), r.faltante());
        assertEquals(d("6.40"), Calculos.costoKgVendido(r, d("5.00"), d("7.00")));
    }

    @Test
    @DisplayName("FIFO: si falta saldo se informa el faltante")
    void consumoConFaltante() {
        ResultadoFifo r = Calculos.consumirFifo(List.of(new LoteKg(1, d("1.80"), d("5.50"))), d("2.00"));
        assertEquals(d("0.20"), r.faltante());
    }

    @Test
    @DisplayName("RF31: el abono se aplica a las ventas más antiguas")
    void aplicarAbono() {
        List<Aplicacion> a = Calculos.aplicarAbono(List.of(new Pendiente(1, d("640.00")), new Pendiente(2, d("732.00"))), d("700.00"));
        assertEquals(2, a.size());
        assertEquals(d("640.00"), a.get(0).monto());
        assertEquals(d("60.00"), a.get(1).monto());
    }

    @Test
    @DisplayName("RF33: tramos de antigüedad")
    void tramos() {
        assertEquals("0–7 días", Calculos.tramo(7));
        assertEquals("8–15 días", Calculos.tramo(8));
        assertEquals("16–30 días", Calculos.tramo(20));
        assertEquals("Más de 30 días", Calculos.tramo(31));
    }

    @Test
    @DisplayName("RF28: color según los días en saldo")
    void colorSaldo() {
        assertEquals("verde", Calculos.colorSaldo(0, 3));
        assertEquals("naranja", Calculos.colorSaldo(2, 3));
        assertEquals("rojo", Calculos.colorSaldo(3, 3));
    }

    @Test
    @DisplayName("RF27 y RF39: diferencias del cierre y del cuadre")
    void diferencias() {
        assertEquals(d("8.00"), Calculos.porcentajeDiferencia(d("9.20"), d("10.00")));
        assertEquals(d("-5.00"), Calculos.diferencia(d("960.40"), d("965.40")));
    }

    @Test
    @DisplayName("RF40: ganancia = ventas − costo − pérdidas − gastos")
    void ganancia() {
        BigDecimal g = Calculos.ganancia(d("2603.20"), d("1912.60"), d("27.60"), d("47.00"));
        assertEquals(d("616.00"), g);
        assertEquals(d("23.7"), Calculos.margen(g, d("2603.20")));
    }

    @Test
    @DisplayName("El RUC debe tener 11 dígitos")
    void ruc() {
        assertTrue(Calculos.rucValido("20601234561"));
        assertEquals(false, Calculos.rucValido("2060123"));
    }
}
