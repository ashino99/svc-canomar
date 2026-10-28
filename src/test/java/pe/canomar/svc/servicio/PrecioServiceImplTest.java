package pe.canomar.svc.servicio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import pe.canomar.svc.comun.Calculos.LoteKg;
import pe.canomar.svc.comun.ConfirmacionException;
import pe.canomar.svc.dao.LoteDao;
import pe.canomar.svc.dao.PrecioDao;
import pe.canomar.svc.modelo.PrecioVista;
import pe.canomar.svc.servicio.impl.PrecioServiceImpl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas de precios del día y costo promedio (RF12, RF15). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PrecioServiceImplTest {

    @Mock PrecioDao precioDao;
    @Mock LoteDao loteDao;
    @Mock AuditoriaService auditoria;
    PrecioServiceImpl servicio;

    private static BigDecimal d(String v) {
        return new BigDecimal(v);
    }

    @BeforeEach
    void preparar() {
        servicio = new PrecioServiceImpl(precioDao, loteDao, auditoria);
        when(precioDao.preciosDe(any())).thenReturn(List.of(new PrecioVista(1, "Bonito", "Bonito", d("6.48"), null, d("9.00"), d("68.00"))));
    }

    @Test
    @DisplayName("RF15 alterno: precio por debajo del costo pide confirmación")
    void bajoCosto() {
        assertThrows(ConfirmacionException.class, () -> servicio.guardarPrecios(Map.of(1, d("6.00")), false, 2));
        assertEquals(1, servicio.guardarPrecios(Map.of(1, d("6.00")), true, 2));
    }

    @Test
    @DisplayName("RF12: el costo promedio se recalcula con el saldo y la compra de hoy")
    void recalcularCosto() {
        when(loteDao.disponiblesFifo(1)).thenReturn(List.of(new LoteKg(1, d("8.00"), d("6.20")), new LoteKg(2, d("60.00"), d("6.50"))));
        when(precioDao.precioDe(eq(1), any())).thenReturn(Optional.empty());
        when(precioDao.ultimoPrecio(eq(1), any())).thenReturn(Optional.of(d("9.00")));
        assertEquals(d("6.46"), servicio.recalcularCosto(1));
        verify(precioDao).guardarCosto(eq(1), any(), eq(d("6.46")), eq(d("9.00")));
    }
}
