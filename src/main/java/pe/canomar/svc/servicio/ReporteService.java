package pe.canomar.svc.servicio;

import pe.canomar.svc.modelo.CierreMensual;
import pe.canomar.svc.modelo.EstadoCuenta;
import pe.canomar.svc.modelo.GananciaProducto;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/** Reportes, exportación a Excel (Apache POI) y PDF del estado de cuenta (RF37, RF45, RF47, RF48). */
public interface ReporteService {
    CierreMensual cierreMensual(YearMonth mes);

    List<GananciaProducto> gananciaPorProducto(LocalDate desde, LocalDate hasta);

    byte[] excelCierreMensual(YearMonth mes);

    byte[] excelGananciaProducto(LocalDate desde, LocalDate hasta);

    byte[] excelVentas(LocalDate desde, LocalDate hasta);

    byte[] pdfEstadoCuenta(EstadoCuenta estado);
}
