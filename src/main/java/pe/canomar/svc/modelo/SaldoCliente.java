package pe.canomar.svc.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import pe.canomar.svc.comun.Dinero;

/** Saldo de un cliente: ventas al crédito menos abonos (RF32). */
public record SaldoCliente(int idCliente, String nombre, String ruc, BigDecimal limite, int plazoDias, BigDecimal saldo, BigDecimal vencido, LocalDate ventaMasAntigua) {
    public BigDecimal disponible() {
        return Dinero.r2(Dinero.nz(limite).subtract(Dinero.nz(saldo)).max(BigDecimal.ZERO));
    }

    public boolean sobreLimite() {
        return Dinero.mayor(saldo, limite);
    }

    public boolean conVencido() {
        return Dinero.positivo(vencido);
    }
}
