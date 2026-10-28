package pe.canomar.svc.web;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pe.canomar.svc.comun.Fechas;
import pe.canomar.svc.seguridad.UsuarioSesion;
import pe.canomar.svc.servicio.AuditoriaService;
import pe.canomar.svc.servicio.ReporteService;

import java.time.LocalDate;
import java.time.YearMonth;

/** P19 Cierre mensual y reportes (RF45, RF47, RF48). Administrador y contador. */
@Controller
@RequestMapping("/reportes")
public class ReporteController {

    private static final MediaType XLSX = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ReporteService reportes;
    private final AuditoriaService auditoria;

    public ReporteController(ReporteService reportes, AuditoriaService auditoria) {
        this.reportes = reportes;
        this.auditoria = auditoria;
    }

    @GetMapping
    public String ver(@RequestParam(required = false) YearMonth mes,
                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta, Model model) {
        YearMonth m = mes == null ? YearMonth.from(Fechas.hoy()) : mes;
        LocalDate h = hasta == null ? Fechas.hoy() : hasta;
        LocalDate d = desde == null ? h.withDayOfMonth(1) : desde;
        model.addAttribute("mes", m);
        model.addAttribute("cierre", reportes.cierreMensual(m));
        model.addAttribute("desde", d);
        model.addAttribute("hasta", h);
        model.addAttribute("productos", reportes.gananciaPorProducto(d, h));
        return "reportes";
    }

    @GetMapping("/excel/cierre")
    public ResponseEntity<byte[]> excelCierre(@AuthenticationPrincipal UsuarioSesion yo, @RequestParam YearMonth mes) {
        auditoria.registrar(yo.getId(), "EXPORTAR_EXCEL", "Cierre mensual " + mes);
        return excel(reportes.excelCierreMensual(mes), "Cierre_" + mes + ".xlsx");
    }

    @GetMapping("/excel/productos")
    public ResponseEntity<byte[]> excelProductos(@AuthenticationPrincipal UsuarioSesion yo,
                                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        auditoria.registrar(yo.getId(), "EXPORTAR_EXCEL", "Ganancia por producto " + desde + " a " + hasta);
        return excel(reportes.excelGananciaProducto(desde, hasta), "GananciaProducto_" + desde + "_" + hasta + ".xlsx");
    }

    @GetMapping("/excel/ventas")
    public ResponseEntity<byte[]> excelVentas(@AuthenticationPrincipal UsuarioSesion yo,
                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        auditoria.registrar(yo.getId(), "EXPORTAR_EXCEL", "Ventas " + desde + " a " + hasta);
        return excel(reportes.excelVentas(desde, hasta), "Ventas_" + desde + "_" + hasta + ".xlsx");
    }

    private ResponseEntity<byte[]> excel(byte[] datos, String nombre) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nombre).build().toString())
                .contentType(XLSX).body(datos);
    }
}
