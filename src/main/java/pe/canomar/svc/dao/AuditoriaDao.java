package pe.canomar.svc.dao;

import pe.canomar.svc.modelo.AuditoriaVista;

import java.time.LocalDate;
import java.util.List;

/** Bitácora de operaciones (auditoria). */
public interface AuditoriaDao {
    void registrar(Integer idUsuario, String operacion, String detalle);

    List<AuditoriaVista> buscar(Integer idUsuario, String operacion, LocalDate desde, LocalDate hasta);

    List<String> operaciones();
}
