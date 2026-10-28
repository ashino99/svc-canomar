package pe.canomar.svc.servicio.impl;

import org.springframework.stereotype.Service;
import pe.canomar.svc.dao.RespaldoDao;
import pe.canomar.svc.servicio.AuditoriaService;
import pe.canomar.svc.servicio.RespaldoService;

import java.nio.charset.StandardCharsets;

@Service
public class RespaldoServiceImpl implements RespaldoService {

    private final RespaldoDao dao;
    private final AuditoriaService auditoria;

    public RespaldoServiceImpl(RespaldoDao dao, AuditoriaService auditoria) {
        this.dao = dao;
        this.auditoria = auditoria;
    }

    @Override
    public byte[] generar(int idActor) {
        byte[] sql = dao.exportarSql().getBytes(StandardCharsets.UTF_8);
        auditoria.registrar(idActor, "RESPALDO", "Respaldo generado (" + (sql.length / 1024) + " KB)");
        return sql;
    }
}
