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
 * RF19 Calcular importe por peso — pruebas TDD de la clase Calculos / VentaServiceImpl.
 * Qué decide: calcula. Un método por caso de prueba (CP) de la ficha del Anexo B.
 */
@DisplayName("RF19 Calcular importe por peso")
class RF19CalcularImportePorPesoTest extends TddBase {

    /** RF19-CP01 (E1). Importe S/ 59.20. */
    @Test
    @DisplayName("RF19-CP01 Calcular 1.85 kg.")
    void rf19_cp01_importeKilosPorPrecio() throws Exception {
        // ARRANGE
        BigDecimal kilos = d("1.85");
        BigDecimal precio = d("32.00");
        // ACT
        BigDecimal importe = Calculos.importe(kilos, precio);
        // ASSERT
        assertEquals(d("59.20"), importe);
    }

    /** RF19-CP02 (E2). Importe S/ 24.00. */
    @Test
    @DisplayName("RF19-CP02 Calcular 4 kg de remate.")
    void rf19_cp02_importeRemate() throws Exception {
        // ARRANGE
        BigDecimal precioRemate = d("6.00");
        // ACT
        BigDecimal importe = Calculos.importe(d("4"), precioRemate);
        // ASSERT
        assertEquals(d("24.00"), importe);
    }

    /** RF19-CP03 (E3). Kilos 1.86. */
    @Test
    @DisplayName("RF19-CP03 Normalizar los kilos.")
    void rf19_cp03_redondeaKilos() throws Exception {
        // ARRANGE
        BigDecimal balanza = d("1.855");
        // ACT
        BigDecimal kilos = Calculos.kilos(balanza);
        // ASSERT
        assertEquals(d("1.86"), kilos);
    }

    /** RF19-CP04 (E4). Mensaje «Pota: ingrese el precio de hoy de este producto». */
    @Test
    @DisplayName("RF19-CP04 Registrar 2 kg de pota.")
    void rf19_cp04_sinPrecioDelDia() throws Exception {
        // ARRANGE
        when(productoDao.buscar(3)).thenReturn(Optional.of(producto(3, "Pota")));
        when(precioDao.precioDe(eq(3), any())).thenReturn(Optional.empty());
        // ACT
        NegocioException e = assertThrows(NegocioException.class,
                () -> ventaService().registrar(contado("EFECTIVO", item(3, "2")), 2, false, false));
        // ASSERT
        assertEquals("Pota: ingrese el precio de hoy de este producto", e.getMessage());
    }
}
