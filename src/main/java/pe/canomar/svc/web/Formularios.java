package pe.canomar.svc.web;

import pe.canomar.svc.comun.NegocioException;

import java.util.List;

/** Ayudas para leer listas paralelas de un formulario (por ejemplo, productos y kilos). */
final class Formularios {
    private Formularios() {
    }

    static <T> T en(List<T> lista, int i) {
        return lista != null && i < lista.size() ? lista.get(i) : null;
    }

    static int tamano(List<?> lista) {
        if (lista == null || lista.isEmpty()) {
            throw new NegocioException("Agregue al menos un producto");
        }
        return lista.size();
    }
}
