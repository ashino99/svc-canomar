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
 * RF49 Generar respaldo de la información — pruebas TDD de la clase RespaldoServiceImpl.
 * Qué decide: cambia estado. Un método por caso de prueba (CP) de la ficha del Anexo B.
 */
@DisplayName("RF49 Generar respaldo de la información")
class RF49GenerarRespaldoDeLaInformacionTest extends TddBase {

    /** RF49-CP01 (E1). El archivo contiene las sentencias INSERT del DAO. */
    @Test
    @DisplayName("RF49-CP01 Generar el respaldo.")
    void rf49_cp01_generaRespaldo() throws Exception {
        // ARRANGE
        when(respaldoDao.exportarSql()).thenReturn("INSERT INTO producto VALUES (1,'Bonito');\n");
        // ACT
        byte[] sql = respaldoService().generar(1);
        // ASSERT
        assertTrue(new String(sql, StandardCharsets.UTF_8).startsWith("INSERT INTO producto"));
    }

    /** RF49-CP02 (E2). Bitácora RESPALDO «Respaldo generado (2 KB)». */
    @Test
    @DisplayName("RF49-CP02 Generar el respaldo.")
    void rf49_cp02_registraEnBitacora() throws Exception {
        // ARRANGE
        when(respaldoDao.exportarSql()).thenReturn("x".repeat(2048));
        // ACT
        respaldoService().generar(1);
        // ASSERT
        verify(auditoria).registrar(1, "RESPALDO", "Respaldo generado (2 KB)");
    }

    /** RF49-CP03 (E3). El texto conserva «Ñaña» en UTF-8. */
    @Test
    @DisplayName("RF49-CP03 Generar el respaldo.")
    void rf49_cp03_utf8() throws Exception {
        // ARRANGE
        when(respaldoDao.exportarSql()).thenReturn("INSERT INTO cliente VALUES (9,'Pollería Doña Ñaña');");
        // ACT
        byte[] sql = respaldoService().generar(1);
        // ASSERT
        assertTrue(new String(sql, StandardCharsets.UTF_8).contains("Doña Ñaña"));
    }
}
