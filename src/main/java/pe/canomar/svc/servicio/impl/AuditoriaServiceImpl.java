package pe.canomar.svc.servicio.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pe.canomar.svc.dao.AuditoriaDao;
import pe.canomar.svc.modelo.AuditoriaVista;
import pe.canomar.svc.servicio.AuditoriaService;

import java.time.LocalDate;
import java.util.List;

@Service
public class AuditoriaServiceImpl implements AuditoriaService {

    private static final Logger log = LoggerFactory.getLogger("BITACORA");
    private final AuditoriaDao dao;

    public AuditoriaServiceImpl(AuditoriaDao dao) {
        this.dao = dao;
    }

    @Override
    public void registrar(Integer idUsuario, String operacion, String detalle) {
        dao.registrar(idUsuario, operacion, detalle);
        log.info("[{}] usuario={} {}", operacion, idUsuario, detalle);
    }

    @Override
    public List<AuditoriaVista> consultar(Integer idUsuario, String operacion, LocalDate desde, LocalDate hasta) {
        if (desde.isAfter(hasta)) {
            throw new pe.canomar.svc.comun.NegocioException("La fecha inicial no puede ser mayor que la final");
        }
        return dao.buscar(idUsuario, operacion, desde, hasta);
    }

    @Override
    public List<String> operaciones() {
        return dao.operaciones();
    }
}
