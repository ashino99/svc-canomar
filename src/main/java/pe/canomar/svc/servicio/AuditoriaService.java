package pe.canomar.svc.servicio;

import pe.canomar.svc.modelo.AuditoriaVista;

import java.time.LocalDate;
import java.util.List;

/** Bitácora de operaciones (RF50). */
public interface AuditoriaService {
    void registrar(Integer idUsuario, String operacion, String detalle);

    List<AuditoriaVista> consultar(Integer idUsuario, String operacion, LocalDate desde, LocalDate hasta);

    List<String> operaciones();
}
