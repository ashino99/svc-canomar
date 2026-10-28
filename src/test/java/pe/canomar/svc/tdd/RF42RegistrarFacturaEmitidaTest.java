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
 * RF42 Registrar factura emitida — pruebas TDD de la clase FacturaServiceImpl.
 * Qué decide: valida. Un método por caso de prueba (CP) de la ficha del Anexo B.
 */
@DisplayName("RF42 Registrar factura emitida")
class RF42RegistrarFacturaEmitidaTest extends TddBase {

    /** RF42-CP01 (E1). Se guarda la factura y la bitácora «E001-000154 · Cevichería El Muelle · S/ 228.00». */
    @Test
    @DisplayName("RF42-CP01 Registrar E001-000154 del 27/10/2026 por S/ 228.00.")
    void rf42_cp01_registraFactura() throws Exception {
        // ARRANGE
        when(clienteDao.buscar(1)).thenReturn(Optional.of(cliente(1, "Cevichería El Muelle", "1500.00", 15)));
        when(facturaDao.insertar(anyInt(), anyString(), anyInt(), any(), any(), anyBoolean())).thenReturn(1);
        // ACT
        facturaService().registrar(1, "e001", 154, LocalDate.of(2026, 10, 27), d("228"), false, 3);
        // ASSERT
        verify(facturaDao).insertar(1, "E001", 154, LocalDate.of(2026, 10, 27), d("228.00"), false);
        verify(auditoria).registrar(3, "REGISTRAR_FACTURA", "E001-000154 · Cevichería El Muelle · S/ 228.00");
    }

    /** RF42-CP02 (E2). Se guarda con detracción = sí. */
    @Test
    @DisplayName("RF42-CP02 Registrar F001-000020 por S/ 732 con detracción.")
    void rf42_cp02_conDetraccion() throws Exception {
        // ARRANGE
        when(clienteDao.buscar(2)).thenReturn(Optional.of(cliente(2, "Restaurante Sabor Norteño", "1500.00", 7)));
        // ACT
        facturaService().registrar(2, "F001", 20, Fechas.hoy(), d("732"), true, 3);
        // ASSERT
        verify(facturaDao).insertar(2, "F001", 20, Fechas.hoy(), d("732.00"), true);
    }

    /** RF42-CP03 (E3). Mensaje «La factura E001-000154 ya está registrada». */
    @Test
    @DisplayName("RF42-CP03 Registrar otra vez E001-000154.")
    void rf42_cp03_facturaDuplicada() throws Exception {
        // ARRANGE
        when(clienteDao.buscar(1)).thenReturn(Optional.of(cliente(1, "Cevichería El Muelle", "1500.00", 15)));
        when(facturaDao.existe("E001", 154)).thenReturn(true);
        // ACT
        NegocioException e = assertThrows(NegocioException.class,
                () -> facturaService().registrar(1, "E001", 154, Fechas.hoy(), d("228"), false, 3));
        // ASSERT
        assertEquals("La factura E001-000154 ya está registrada", e.getMessage());
    }

    /** RF42-CP04 (E4). Mensaje «El cliente no tiene RUC; no se puede registrar factura». */
    @Test
    @DisplayName("RF42-CP04 Registrar una factura a la pollería.")
    void rf42_cp04_clienteSinRuc() throws Exception {
        // ARRANGE
        when(clienteDao.buscar(3)).thenReturn(Optional.of(new Cliente(3, "Pollería Don Lucho", null, null, d("800.00"), 7, "ACTIVO")));
        // ACT
        NegocioException e = assertThrows(NegocioException.class,
                () -> facturaService().registrar(3, "E001", 155, Fechas.hoy(), d("50"), false, 3));
        // ASSERT
        assertEquals("El cliente no tiene RUC; no se puede registrar factura", e.getMessage());
    }
}
