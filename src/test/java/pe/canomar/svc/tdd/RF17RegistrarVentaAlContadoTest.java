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
 * RF17 Registrar venta al contado — pruebas TDD de la clase VentaServiceImpl.
 * Qué decide: valida. Un método por caso de prueba (CP) de la ficha del Anexo B.
 */
@DisplayName("RF17 Registrar venta al contado")
class RF17RegistrarVentaAlContadoTest extends TddBase {

    /** RF17-CP01 (E1). Se guarda la venta CONTADO · YAPE, se descuentan 1.85 kg del lote 10 y el detalle queda con costo S/ 26.00. */
    @Test
    @DisplayName("RF17-CP01 Vender 1.85 kg de corvina al contado por Yape.")
    void rf17_cp01_ventaContadoYape() throws Exception {
        // ARRANGE
        prepararCorvina();
        // ACT
        ventaService().registrar(contado("YAPE", item(2, "1.85")), 2, false, false);
        // ASSERT
        verify(ventaDao).insertarVenta(any(), eq(2), isNull());
        verify(loteDao).descontar(10, d("1.85"));
        verify(ventaDao).insertarDetalle(7L, 2, d("1.85"), d("32.00"), d("26.00"));
    }

    /** RF17-CP02 (E2). Se guarda una venta con 2 detalles; total S/ 91.00. */
    @Test
    @DisplayName("RF17-CP02 Vender 2 kg de corvina y 3 kg de bonito en una venta.")
    void rf17_cp02_variosProductos() throws Exception {
        // ARRANGE
        prepararCorvina();
        prepararBonito();
        // ACT
        ventaService().registrar(contado("EFECTIVO", item(2, "2"), item(1, "3")), 2, false, false);
        // ASSERT
        ArgumentCaptor<NuevaVenta> v = ArgumentCaptor.forClass(NuevaVenta.class);
        verify(ventaDao).insertarVenta(v.capture(), eq(2), isNull());
        assertEquals(d("91.00"), v.getValue().total());
        verify(ventaDao, times(2)).insertarDetalle(eq(7L), anyInt(), any(), any(), any());
    }

    /** RF17-CP03 (E3). Pide confirmar: «Solo quedan 15.00 kg de corvina. ¿Registrar igual?»; no se guarda. */
    @Test
    @DisplayName("RF17-CP03 Vender 20 kg sin confirmar.")
    void rf17_cp03_superaSaldoPideConfirmar() throws Exception {
        // ARRANGE
        prepararCorvina();
        // ACT
        ConfirmacionException e = assertThrows(ConfirmacionException.class,
                () -> ventaService().registrar(contado("EFECTIVO", item(2, "20")), 2, false, false));
        // ASSERT
        assertEquals("Solo quedan 15.00 kg de corvina. ¿Registrar igual?", e.getMessage());
        verify(ventaDao, never()).insertarVenta(any(), anyInt(), any());
    }

    /** RF17-CP04 (E4). Mensaje «Corvina: ingrese los kilos de la balanza». */
    @Test
    @DisplayName("RF17-CP04 Registrar la corvina con 0 kg.")
    void rf17_cp04_faltanKilos() throws Exception {
        // ARRANGE
        prepararCorvina();
        // ACT
        NegocioException e = assertThrows(NegocioException.class,
                () -> ventaService().registrar(contado("EFECTIVO", item(2, "0")), 2, false, false));
        // ASSERT
        assertEquals("Corvina: ingrese los kilos de la balanza", e.getMessage());
    }
}
