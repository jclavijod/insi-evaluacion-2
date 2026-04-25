package cl.iplacex.tiendaweb.service;

import java.util.List;

import cl.iplacex.tiendaweb.domain.Categoria;
import cl.iplacex.tiendaweb.domain.CategoriaImpl;
import org.springframework.stereotype.Service;

@Service
public class CategoriaServiceImpl implements CategoriaService {

    private static final List<Categoria> CATEGORIAS = List.of(
            new CategoriaImpl(1, "Notebooks"),
            new CategoriaImpl(2, "Smartwatches"),
            new CategoriaImpl(3, "Memorias"),
            new CategoriaImpl(4, "Impresoras"),
            new CategoriaImpl(5, "Mouses y Teclados")
    );

    @Override
    public List<? extends Categoria> getCategorias() {
        return CATEGORIAS;
    }

    @Override
    public Categoria getCategoriaById(int id) {
        return CATEGORIAS.stream()
                .filter(c -> c.getId() == id)
                .findFirst()
                .orElse(null);
    }
}
