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
 * RF29 Alertar mercadería antigua — pruebas TDD de la clase SaldoServiceImpl.
 * Qué decide: valida. Un método por caso de prueba (CP) de la ficha del Anexo B.
 */
@DisplayName("RF29 Alertar mercadería antigua")
class RF29AlertarMercaderiaAntiguaTest extends TddBase {

    /** RF29-CP01 (E1). Solo aparece el bonito. */
    @Test
    @DisplayName("RF29-CP01 Consultar la mercadería antigua.")
    void rf29_cp01_alertaSoloAntiguos() throws Exception {
        // ARRANGE
        when(configDao.obtener()).thenReturn(config());
        when(loteDao.lotesConKilos()).thenReturn(List.of(lote(1, 1, "Bonito", 3, "8.00", "6.50"), lote(2, 5, "Jurel", 2, "6.00", "5.00")));
        // ACT
        List<SaldoLinea> antiguos = saldoService().mercaderiaAntigua();
        // ASSERT
        assertEquals(1, antiguos.size());
        assertEquals("Bonito", antiguos.get(0).lote().producto());
    }

    /** RF29-CP02 (E2). Lista vacía: no hay alerta. */
    @Test
    @DisplayName("RF29-CP02 Consultar la mercadería antigua.")
    void rf29_cp02_sinAntiguos() throws Exception {
        // ARRANGE
        when(configDao.obtener()).thenReturn(config());
        when(loteDao.lotesConKilos()).thenReturn(List.of(lote(7, 2, "Corvina", 0, "40.00", "26.00")));
        // ACT
        List<SaldoLinea> antiguos = saldoService().mercaderiaAntigua();
        // ASSERT
        assertTrue(antiguos.isEmpty());
    }

    /** RF29-CP03 (E3). Aparece el jurel. */
    @Test
    @DisplayName("RF29-CP03 Consultar la mercadería antigua.")
    void rf29_cp03_usaDiasConfigurados() throws Exception {
        // ARRANGE
        when(configDao.obtener()).thenReturn(new Configuracion(2, d("5.00"), d("5.00")));
        when(loteDao.lotesConKilos()).thenReturn(List.of(lote(2, 5, "Jurel", 2, "6.00", "5.00")));
        // ACT
        List<SaldoLinea> antiguos = saldoService().mercaderiaAntigua();
        // ASSERT
        assertEquals(1, antiguos.size());
    }
}
