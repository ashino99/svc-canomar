package pe.canomar.svc.dao;

import pe.canomar.svc.modelo.CuadreVista;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Acceso a cuadre. */
public interface CuadreDao {
    int insertar(int idUsuario, LocalDate fecha, BigDecimal efectivoEsperado, BigDecimal efectivoContado,
                 BigDecimal digitalEsperado, BigDecimal digitalRevisado, String motivo, BigDecimal ganancia);

    Optional<CuadreVista> buscar(LocalDate fecha);

    List<CuadreVista> listarEntre(LocalDate desde, LocalDate hasta);

    int contarEntre(LocalDate desde, LocalDate hasta);
}
