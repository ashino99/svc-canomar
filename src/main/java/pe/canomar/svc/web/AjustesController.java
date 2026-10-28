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
import pe.canomar.svc.servicio.RespaldoService;
import pe.canomar.svc.servicio.UsuarioService;

import java.time.LocalDate;

/** P20 Respaldo (RF49) y bitácora (RF50). Administrador. */
@Controller
@RequestMapping("/ajustes")
public class AjustesController {

    private final RespaldoService respaldo;
    private final AuditoriaService auditoria;
    private final UsuarioService usuarios;

    public AjustesController(RespaldoService respaldo, AuditoriaService auditoria, UsuarioService usuarios) {
        this.respaldo = respaldo;
        this.auditoria = auditoria;
        this.usuarios = usuarios;
    }

    @GetMapping
    public String ver(@RequestParam(required = false) Integer usuario, @RequestParam(required = false) String operacion,
                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta, Model model) {
        LocalDate h = hasta == null ? Fechas.hoy() : hasta;
        LocalDate d = desde == null ? h : desde;
        model.addAttribute("eventos", auditoria.consultar(usuario, operacion, d, h));
        model.addAttribute("operaciones", auditoria.operaciones());
        model.addAttribute("usuarios", usuarios.listar());
        model.addAttribute("fUsuario", usuario);
        model.addAttribute("fOperacion", operacion);
        model.addAttribute("desde", d);
        model.addAttribute("hasta", h);
        return "ajustes";
    }

    @GetMapping("/respaldo")
    public ResponseEntity<byte[]> respaldo(@AuthenticationPrincipal UsuarioSesion yo) {
        byte[] sql = respaldo.generar(yo.getId());
        String nombre = "respaldo_svc_canomar_" + Fechas.ahora().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".sql";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nombre).build().toString())
                .contentType(MediaType.parseMediaType("application/sql")).body(sql);
    }
}
