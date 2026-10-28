package pe.canomar.svc.servicio.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.canomar.svc.comun.Calculos;
import pe.canomar.svc.comun.Dinero;
import pe.canomar.svc.comun.NegocioException;
import pe.canomar.svc.dao.ClienteDao;
import pe.canomar.svc.modelo.Cliente;
import pe.canomar.svc.modelo.SaldoCliente;
import pe.canomar.svc.servicio.AuditoriaService;
import pe.canomar.svc.servicio.ClienteService;
import pe.canomar.svc.servicio.CreditoService;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ClienteServiceImpl implements ClienteService {

    private final ClienteDao dao;
    private final CreditoService credito;
    private final AuditoriaService auditoria;

    public ClienteServiceImpl(ClienteDao dao, CreditoService credito, AuditoriaService auditoria) {
        this.dao = dao;
        this.credito = credito;
        this.auditoria = auditoria;
    }

    @Override
    public List<Cliente> listar() {
        return dao.listar();
    }

    @Override
    public List<Cliente> activos() {
        return dao.listarActivos();
    }

    @Override
    public Cliente buscar(int idCliente) {
        return dao.buscar(idCliente).orElseThrow(() -> new NegocioException("El cliente no existe"));
    }

    @Override
    @Transactional
    public int registrar(String nombre, String ruc, String telefono, BigDecimal limite, int plazoDias, int idActor) {
        Cliente c = validar(null, nombre, ruc, telefono, limite, plazoDias, "ACTIVO");
        int id = dao.insertar(c);
        auditoria.registrar(idActor, "REGISTRAR_CLIENTE", c.nombreComercial() + " · límite S/ " + c.limiteCredito());
        return id;
    }

    @Override
    @Transactional
    public String editar(int idCliente, String nombre, String ruc, String telefono, BigDecimal limite, int plazoDias, String estado, int idActor) {
        Cliente anterior = buscar(idCliente);
        Cliente c = validar(idCliente, nombre, ruc, telefono, limite, plazoDias, estado);
        dao.actualizar(c);
        if (anterior.limiteCredito().compareTo(c.limiteCredito()) != 0) {
            auditoria.registrar(idActor, "CAMBIO_LIMITE", c.nombreComercial() + ": S/ " + anterior.limiteCredito() + " → S/ " + c.limiteCredito());
        } else {
            auditoria.registrar(idActor, "EDITAR_CLIENTE", c.nombreComercial());
        }
        SaldoCliente s = credito.saldo(idCliente);
        if (Dinero.mayor(s.saldo(), c.limiteCredito())) {
            return "El saldo actual (S/ " + s.saldo() + ") supera el nuevo límite: no podrá comprar al crédito hasta abonar";
        }
        return null;
    }

    private Cliente validar(Integer id, String nombre, String ruc, String telefono, BigDecimal limite, int plazoDias, String estado) {
        String n = StringUtils.normalizeSpace(nombre);
        if (StringUtils.isBlank(n)) {
            throw new NegocioException("Ingrese el nombre del cliente");
        }
        String r = StringUtils.trimToNull(ruc);
        if (r != null && !Calculos.rucValido(r)) {
            throw new NegocioException("El RUC debe tener 11 dígitos");
        }
        if (r != null && dao.existeRuc(r, id)) {
            throw new NegocioException("El cliente ya está registrado con ese RUC");
        }
        BigDecimal lim = Dinero.r2(limite);
        if (lim.signum() < 0) {
            throw new NegocioException("El límite de crédito no puede ser negativo");
        }
        if (plazoDias < 0 || plazoDias > 90) {
            throw new NegocioException("El plazo debe estar entre 0 y 90 días");
        }
        return new Cliente(id, n, r, StringUtils.trimToNull(telefono), lim, plazoDias, "INACTIVO".equals(estado) ? "INACTIVO" : "ACTIVO");
    }
}
