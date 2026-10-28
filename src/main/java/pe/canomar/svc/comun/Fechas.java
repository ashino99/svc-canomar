package pe.canomar.svc.comun;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/** Fechas siempre en la zona horaria de Lima. */
public final class Fechas {
    public static final ZoneId ZONA = ZoneId.of("America/Lima");
    public static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter DIA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Fechas() {
    }

    public static LocalDate hoy() {
        return LocalDate.now(ZONA);
    }

    public static LocalDateTime ahora() {
        return LocalDateTime.now(ZONA).withNano(0);
    }

    public static long diasEntre(LocalDate desde, LocalDate hasta) {
        return ChronoUnit.DAYS.between(desde, hasta);
    }
}
