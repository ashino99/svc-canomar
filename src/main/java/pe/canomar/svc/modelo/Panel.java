package pe.canomar.svc.modelo;

import java.math.BigDecimal;
import java.util.List;

/** Indicadores del panel del negocio (RF46). */
public record Panel(BigDecimal ventasHoy, int ventasHoyCantidad, BigDecimal gananciaHoy, BigDecimal porCobrar, BigDecimal deudaProveedores, BigDecimal kilosSaldo, List<SaldoLinea> mercaderiaAntigua, List<SaldoCliente> clientesVencidos) {
}
