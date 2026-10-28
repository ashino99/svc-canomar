package pe.canomar.svc.servicio.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.canomar.svc.comun.Dinero;
import pe.canomar.svc.comun.NegocioException;
import pe.canomar.svc.dao.LoteDao;
import pe.canomar.svc.dao.ProductoDao;
import pe.canomar.svc.modelo.Producto;
import pe.canomar.svc.servicio.AuditoriaService;
import pe.canomar.svc.servicio.ProductoService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Service
public class ProductoServiceImpl implements ProductoService {

    private static final Set<String> CATEGORIAS = Set.of("PESCADO", "MARISCO", "MOLUSCO", "OTRO");

    private final ProductoDao dao;
    private final LoteDao loteDao;
    private final AuditoriaService auditoria;

    public ProductoServiceImpl(ProductoDao dao, LoteDao loteDao, AuditoriaService auditoria) {
        this.dao = dao;
        this.loteDao = loteDao;
        this.auditoria = auditoria;
    }

    @Override
    public List<Producto> listar() {
        return dao.listar();
    }

    @Override
    public List<Producto> activos() {
        return dao.listarActivos();
    }

    @Override
    public Producto buscar(int idProducto) {
        return dao.buscar(idProducto).orElseThrow(() -> new NegocioException("El producto no existe"));
    }

    @Override
    @Transactional
    public int registrar(String nombre, String nombreCorto, String categoria, int idActor) {
        Producto p = validar(null, nombre, nombreCorto, categoria, "ACTIVO");
        int id = dao.insertar(p);
        auditoria.registrar(idActor, "REGISTRAR_PRODUCTO", p.nombre());
        return id;
    }

    @Override
    @Transactional
    public void editar(int idProducto, String nombre, String nombreCorto, String categoria, String estado, int idActor) {
        buscar(idProducto);
        Producto p = validar(idProducto, nombre, nombreCorto, categoria, estado);
        if ("INACTIVO".equals(estado)) {
            BigDecimal kilos = loteDao.kilosDisponibles(idProducto);
            if (Dinero.positivo(kilos)) {
                throw new NegocioException("No se puede inactivar: tiene " + kilos + " kg en saldo");
            }
        }
        dao.actualizar(p);
        auditoria.registrar(idActor, "EDITAR_PRODUCTO", p.nombre() + " · " + estado);
    }

    private Producto validar(Integer id, String nombre, String nombreCorto, String categoria, String estado) {
        String n = StringUtils.capitalize(StringUtils.normalizeSpace(nombre));
        if (StringUtils.isBlank(n)) {
            throw new NegocioException("Ingrese el nombre del producto");
        }
        if (!CATEGORIAS.contains(categoria)) {
            throw new NegocioException("Elija una categoría válida");
        }
        if (dao.existeNombre(n, id)) {
            throw new NegocioException(id == null ? "El producto ya existe" : "Ya existe otro producto con ese nombre");
        }
        String corto = StringUtils.defaultIfBlank(StringUtils.normalizeSpace(nombreCorto), StringUtils.left(n, 20));
        return new Producto(id, n, StringUtils.left(corto, 20), categoria, "INACTIVO".equals(estado) ? "INACTIVO" : "ACTIVO");
    }
}
