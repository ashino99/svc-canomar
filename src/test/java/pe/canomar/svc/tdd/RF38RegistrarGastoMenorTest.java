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
 * RF38 Registrar gasto menor — pruebas TDD de la clase CajaServiceImpl.
 * Qué decide: valida. Un método por caso de prueba (CP) de la ficha del Anexo B.
 */
@DisplayName("RF38 Registrar gasto menor")
class RF38RegistrarGastoMenorTest extends TddBase {

    /** RF38-CP01 (E1). Se guarda el gasto de hoy. */
    @Test
    @DisplayName("RF38-CP01 Registrar HIELO S/ 15 en efectivo.")
    void rf38_cp01_registraGasto() throws Exception {
        // ARRANGE
        // ACT
        cajaService().registrarGasto("HIELO", null, d("15"), "EFECTIVO", 2);
        // ASSERT
        verify(gastoDao).insertar(2, Fechas.hoy(), "HIELO", null, d("15.00"), "EFECTIVO");
        verify(auditoria).registrar(2, "REGISTRAR_GASTO", "HIELO · S/ 15.00 · EFECTIVO");
    }

    /** RF38-CP02 (E2). Se guarda con su descripción. */
    @Test
    @DisplayName("RF38-CP02 Registrar OTRO «Cuchillo nuevo» S/ 25 por Yape.")
    void rf38_cp02_otroConDescripcion() throws Exception {
        // ARRANGE
        // ACT
        cajaService().registrarGasto("OTRO", "Cuchillo nuevo", d("25"), "YAPE", 2);
        // ASSERT
        verify(gastoDao).insertar(2, Fechas.hoy(), "OTRO", "Cuchillo nuevo", d("25.00"), "YAPE");
    }

    /** RF38-CP03 (E3). Mensaje «Ingrese un monto mayor que cero». */
    @Test
    @DisplayName("RF38-CP03 Registrar BOLSAS S/ 0.")
    void rf38_cp03_montoCero() throws Exception {
        // ARRANGE
        // ACT
        NegocioException e = assertThrows(NegocioException.class, () -> cajaService().registrarGasto("BOLSAS", null, d("0"), "EFECTIVO", 2));
        // ASSERT
        assertEquals("Ingrese un monto mayor que cero", e.getMessage());
    }

    /** RF38-CP04 (E4). Mensaje «Describa el gasto». */
    @Test
    @DisplayName("RF38-CP04 Registrar OTRO sin descripción.")
    void rf38_cp04_otroSinDescripcion() throws Exception {
        // ARRANGE
        // ACT
        NegocioException e = assertThrows(NegocioException.class, () -> cajaService().registrarGasto("OTRO", " ", d("10"), "EFECTIVO", 2));
        // ASSERT
        assertEquals("Describa el gasto", e.getMessage());
    }
}
