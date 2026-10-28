package pe.canomar.svc.tdd;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pe.canomar.svc.comun.Calculos;
import pe.canomar.svc.comun.Calculos.LoteKg;
import pe.canomar.svc.comun.ConfirmacionException;
import pe.canomar.svc.comun.Fechas;
import pe.canomar.svc.comun.NegocioException;
import pe.canomar.svc.modelo.*;
import pe.canomar.svc.seguridad.UsuarioSesion;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * RF25 Sincronizar ventas pendientes — pruebas TDD de la clase VentaServiceImpl.
 * Qué decide: cambia estado. Un método por caso de prueba (CP) de la ficha del Anexo B.
 */
@DisplayName("RF25 Sincronizar ventas pendientes")
class RF25SincronizarVentasPendientesTest extends TddBase {

    /** RF25-CP01 (E1). Resultado SINCRONIZADA «Venta 0007»; la venta se guarda con fecha de sincronización. */
    @Test
    @DisplayName("RF25-CP01 Sincronizar.")
    void rf25_cp01_sincronizaVenta() throws Exception {
        // ARRANGE
        prepararCorvina();
        NuevaVenta enCola = new NuevaVenta("cel-0001", Fechas.hoy().atTime(6, 40), "CONTADO", "EFECTIVO", null, false, List.of(item(2, "1")));
        // ACT
        List<ResultadoSync> r = ventaService().sincronizar(List.of(enCola), 2);
        // ASSERT
        assertEquals("SINCRONIZADA", r.get(0).estado());
        assertEquals("Venta 0007", r.get(0).mensaje());
        verify(ventaDao).insertarVenta(argThat(v -> v.fechaHora().getHour() == 6), eq(2), notNull());
    }

    /** RF25-CP02 (E2). Resultado DUPLICADA «Ya estaba registrada»; no se inserta. */
    @Test
    @DisplayName("RF25-CP02 Sincronizar otra vez la misma venta.")
    void rf25_cp02_noDuplica() throws Exception {
        // ARRANGE
        when(ventaDao.existeUuid("cel-0001")).thenReturn(true);
        NuevaVenta repetida = new NuevaVenta("cel-0001", Fechas.ahora(), "CONTADO", "EFECTIVO", null, false, List.of(item(2, "1")));
        // ACT
        List<ResultadoSync> r = ventaService().sincronizar(List.of(repetida), 2);
        // ASSERT
        assertEquals("DUPLICADA", r.get(0).estado());
        assertEquals("Ya estaba registrada", r.get(0).mensaje());
        verify(ventaDao, never()).insertarVenta(any(), anyInt(), any());
    }

    /** RF25-CP03 (E3). La válida queda SINCRONIZADA; la otra RECHAZADA «Corvina: ingrese los kilos de la balanza». */
    @Test
    @DisplayName("RF25-CP03 Sincronizar las dos.")
    void rf25_cp03_rechazaUnaYGuardaOtra() throws Exception {
        // ARRANGE
        prepararCorvina();
        NuevaVenta buena = new NuevaVenta("cel-0002", Fechas.ahora(), "CONTADO", "EFECTIVO", null, false, List.of(item(2, "1")));
        NuevaVenta mala = new NuevaVenta("cel-0003", Fechas.ahora(), "CONTADO", "EFECTIVO", null, false, List.of(item(2, "0")));
        // ACT
        List<ResultadoSync> r = ventaService().sincronizar(List.of(buena, mala), 2);
        // ASSERT
        assertEquals("SINCRONIZADA", r.get(0).estado());
        assertEquals("RECHAZADA", r.get(1).estado());
        assertEquals("Corvina: ingrese los kilos de la balanza", r.get(1).mensaje());
    }

    /** RF25-CP04 (E4). Resultado RECHAZADA «La venta no tiene identificador». */
    @Test
    @DisplayName("RF25-CP04 Sincronizar.")
    void rf25_cp04_sinIdentificador() throws Exception {
        // ARRANGE
        NuevaVenta sinId = new NuevaVenta(" ", Fechas.ahora(), "CONTADO", "EFECTIVO", null, false, List.of(item(2, "1")));
        // ACT
        List<ResultadoSync> r = ventaService().sincronizar(List.of(sinId), 2);
        // ASSERT
        assertEquals("RECHAZADA", r.get(0).estado());
        assertEquals("La venta no tiene identificador", r.get(0).mensaje());
    }
}
