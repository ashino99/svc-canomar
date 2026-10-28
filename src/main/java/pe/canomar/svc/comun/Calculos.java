package pe.canomar.svc.comun;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Reglas de cálculo del negocio, sin dependencias de base de datos ni de Spring.
 * Al ser funciones puras se prueban primero con JUnit (TDD) y luego las usan los servicios.
 */
public final class Calculos {

    private Calculos() {
    }

    /** Lote con kilos disponibles y su costo por kilo, ordenado del más antiguo al más reciente. */
    public record LoteKg(int idLote, BigDecimal kilos, BigDecimal costoKg) {
    }

    /** Kilos que se toman de un lote. */
    public record Consumo(int idLote, BigDecimal kilos, BigDecimal costoKg) {
    }

    /** Resultado de descontar kilos por FIFO: lo consumido y lo que faltó. */
    public record ResultadoFifo(List<Consumo> consumos, BigDecimal faltante) {
        public BigDecimal costoTotal() {
            return consumos.stream().map(c -> c.kilos().multiply(c.costoKg())).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    /** Venta al crédito con lo que aún se debe. */
    public record Pendiente(long idVenta, BigDecimal pendiente) {
    }

    /** Parte de un abono aplicada a una venta. */
    public record Aplicacion(long idVenta, BigDecimal monto) {
    }

    /** RF19: importe = kilos × precio por kilo, redondeado a 2 decimales. */
    public static BigDecimal importe(BigDecimal kilos, BigDecimal precioKg) {
        Preconditions.checkArgument(Dinero.positivo(kilos), "Los kilos deben ser mayores que cero");
        Preconditions.checkArgument(precioKg != null && precioKg.signum() >= 0, "El precio no puede ser negativo");
        return kilos.multiply(precioKg).setScale(2, RoundingMode.HALF_UP);
    }

    /** Kilos de la balanza ajustados a 2 decimales (1.855 → 1.86). */
    public static BigDecimal kilos(BigDecimal kilos) {
        return Dinero.r2(kilos);
    }

    /** RF12: costo promedio ponderado por kilo de los lotes con saldo. */
    public static BigDecimal costoPromedio(List<LoteKg> lotes) {
        BigDecimal kilos = BigDecimal.ZERO;
        BigDecimal costo = BigDecimal.ZERO;
        for (LoteKg l : lotes) {
            if (Dinero.positivo(l.kilos())) {
                kilos = kilos.add(l.kilos());
                costo = costo.add(l.kilos().multiply(l.costoKg()));
            }
        }
        if (kilos.signum() == 0) {
            return Dinero.CERO;
        }
        return costo.divide(kilos, 2, RoundingMode.HALF_UP);
    }

    /** Costo por kilo de un lote comprado: costo total ÷ kilos. */
    public static BigDecimal costoPorKilo(BigDecimal costoTotal, BigDecimal kilos) {
        Preconditions.checkArgument(Dinero.positivo(kilos), "Los kilos deben ser mayores que cero");
        return costoTotal.divide(kilos, 2, RoundingMode.HALF_UP);
    }

    /** Descuenta kilos de los lotes, primero del más antiguo (FIFO). */
    public static ResultadoFifo consumirFifo(List<LoteKg> lotesAntiguosPrimero, BigDecimal kilos) {
        Preconditions.checkArgument(Dinero.positivo(kilos), "Los kilos deben ser mayores que cero");
        List<Consumo> consumos = new ArrayList<>();
        BigDecimal falta = kilos;
        for (LoteKg l : lotesAntiguosPrimero) {
            if (falta.signum() <= 0) {
                break;
            }
            if (!Dinero.positivo(l.kilos())) {
                continue;
            }
            BigDecimal toma = l.kilos().min(falta);
            consumos.add(new Consumo(l.idLote(), toma, l.costoKg()));
            falta = falta.subtract(toma);
        }
        return new ResultadoFifo(ImmutableList.copyOf(consumos), Dinero.r2(falta.max(BigDecimal.ZERO)));
    }

    /** Costo por kilo de lo vendido según lo consumido; si faltó saldo se usa el costo promedio. */
    public static BigDecimal costoKgVendido(ResultadoFifo r, BigDecimal kilosVendidos, BigDecimal costoPromedio) {
        BigDecimal total = r.costoTotal().add(r.faltante().multiply(Dinero.nz(costoPromedio)));
        return total.divide(kilosVendidos, 2, RoundingMode.HALF_UP);
    }

    /** RF31: aplica un abono a las ventas más antiguas primero. */
    public static List<Aplicacion> aplicarAbono(List<Pendiente> antiguasPrimero, BigDecimal monto) {
        Preconditions.checkArgument(Dinero.positivo(monto), "El monto debe ser mayor que cero");
        List<Aplicacion> out = new ArrayList<>();
        BigDecimal resto = monto;
        for (Pendiente p : antiguasPrimero) {
            if (resto.signum() <= 0) {
                break;
            }
            if (!Dinero.positivo(p.pendiente())) {
                continue;
            }
            BigDecimal aplica = p.pendiente().min(resto);
            out.add(new Aplicacion(p.idVenta(), Dinero.r2(aplica)));
            resto = resto.subtract(aplica);
        }
        return ImmutableList.copyOf(out);
    }

    /** RF33: tramo de antigüedad de una deuda según sus días. */
    public static String tramo(long dias) {
        if (dias <= 7) {
            return "0–7 días";
        }
        if (dias <= 15) {
            return "8–15 días";
        }
        if (dias <= 30) {
            return "16–30 días";
        }
        return "Más de 30 días";
    }

    /** RF28: color de la antigüedad del saldo de mercadería. */
    public static String colorSaldo(long dias, int diasAlerta) {
        if (dias >= diasAlerta) {
            return "rojo";
        }
        if (dias >= diasAlerta - 1 && dias > 0) {
            return "naranja";
        }
        return "verde";
    }

    /** RF27: diferencia porcentual entre lo real y lo esperado (valor absoluto). */
    public static BigDecimal porcentajeDiferencia(BigDecimal real, BigDecimal esperado) {
        if (!Dinero.positivo(esperado)) {
            return Dinero.positivo(real) ? Dinero.CIEN : Dinero.CERO;
        }
        return real.subtract(esperado).abs().multiply(Dinero.CIEN).divide(esperado, 2, RoundingMode.HALF_UP);
    }

    /** RF39: diferencia del cuadre (contado − esperado). */
    public static BigDecimal diferencia(BigDecimal contado, BigDecimal esperado) {
        return Dinero.r2(Dinero.nz(contado).subtract(Dinero.nz(esperado)));
    }

    /** RF40: ganancia = ventas − costo de lo vendido − pérdidas − gastos. */
    public static BigDecimal ganancia(BigDecimal ventas, BigDecimal costoVendido, BigDecimal perdidas, BigDecimal gastos) {
        return Dinero.r2(Dinero.nz(ventas).subtract(Dinero.nz(costoVendido)).subtract(Dinero.nz(perdidas)).subtract(Dinero.nz(gastos)));
    }

    /** Margen en porcentaje sobre las ventas. */
    public static BigDecimal margen(BigDecimal ganancia, BigDecimal ventas) {
        if (!Dinero.positivo(ventas)) {
            return Dinero.CERO;
        }
        return ganancia.multiply(Dinero.CIEN).divide(ventas, 1, RoundingMode.HALF_UP);
    }

    /** Valida el RUC peruano: 11 dígitos. */
    public static boolean rucValido(String ruc) {
        return ruc != null && ruc.matches("\\d{11}");
    }
}
