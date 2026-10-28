package pe.canomar.svc.dao;

import pe.canomar.svc.comun.Calculos.LoteKg;
import pe.canomar.svc.modelo.LoteVista;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Saldo de mercadería por lotes (lote_saldo) y sus ajustes (ajuste_saldo). */
public interface LoteDao {
    int insertar(int idProducto, Integer idDetalleCompra, LocalDate fechaIngreso, BigDecimal kilos, BigDecimal costoKg);

    /** Lotes con kilos de un producto, del más antiguo al más reciente. */
    List<LoteKg> disponiblesFifo(int idProducto);

    List<LoteVista> lotesConKilos();

    void descontar(int idLote, BigDecimal kilos);

    void sumar(int idLote, BigDecimal kilos);

    Optional<Integer> ultimoLote(int idProducto);

    BigDecimal kilosDisponibles(int idProducto);

    Map<Integer, BigDecimal> kilosPorProducto();

    int insertarAjuste(int idLote, int idUsuario, LocalDateTime fecha, String tipo, BigDecimal kilos, BigDecimal valorCosto, String motivo);

    /** Valor al costo de autoconsumo, mermas, desechos y diferencias de cierre en el periodo. */
    BigDecimal perdidas(LocalDate desde, LocalDate hasta);
}
