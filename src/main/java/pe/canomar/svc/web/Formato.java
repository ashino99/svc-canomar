package pe.canomar.svc.web;

import org.springframework.stereotype.Component;
import pe.canomar.svc.comun.Dinero;
import pe.canomar.svc.comun.Fechas;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

/** Formatos para las vistas: ${@fmt.soles(monto)}, ${@fmt.kg(kilos)}, ${@fmt.fecha(dia)}. */
@Component("fmt")
public class Formato {

    private static final DecimalFormatSymbols SIMBOLOS = DecimalFormatSymbols.getInstance(Locale.US);

    public String soles(BigDecimal v) {
        return v == null ? "—" : (v.signum() < 0 ? "− S/ " : "S/ ") + numero(v.abs());
    }

    public String numero(BigDecimal v) {
        return v == null ? "" : new DecimalFormat("#,##0.00", SIMBOLOS).format(Dinero.r2(v));
    }

    public String plano(BigDecimal v) {
        return v == null ? "" : Dinero.r2(v).toPlainString();
    }

    public String kg(BigDecimal v) {
        return v == null ? "—" : numero(v) + " kg";
    }

    public String fecha(LocalDate d) {
        return d == null ? "—" : d.format(Fechas.DIA);
    }

    public String fechaHora(LocalDateTime d) {
        return d == null ? "—" : d.format(Fechas.DIA_HORA);
    }

    public String hora(LocalDateTime d) {
        return d == null ? "" : d.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
    }

    public long dias(LocalDate d) {
        return d == null ? 0 : Fechas.diasEntre(d, Fechas.hoy());
    }

    public String medio(String m) {
        if (m == null) {
            return "";
        }
        return switch (m) {
            case "YAPE" -> "Yape/Plin";
            case "TRANSFERENCIA" -> "Transferencia";
            case "EFECTIVO" -> "Efectivo";
            default -> m;
        };
    }
}
