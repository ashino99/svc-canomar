package pe.canomar.svc.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import pe.canomar.svc.comun.Dinero;

/** Deuda con un proveedor: compras a crédito menos pagos (RF14). */
public record DeudaProveedor(int idProveedor, String proveedor, String tipo, BigDecimal compras, BigDecimal pagos, LocalDate compraMasAntigua) {
    public BigDecimal deuda() {
        return Dinero.r2(Dinero.nz(compras).subtract(Dinero.nz(pagos)));
    }
}
