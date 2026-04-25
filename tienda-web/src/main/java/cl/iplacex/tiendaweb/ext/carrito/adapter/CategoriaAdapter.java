package cl.iplacex.tiendaweb.ext.carrito.adapter;

import cl.iplacex.tiendaweb.domain.Categoria;
import cl.iplacex.tiendaweb.domain.CategoriaImpl;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;

public class CategoriaAdapter extends XmlAdapter<CategoriaImpl, Categoria> {

    @Override
    public Categoria unmarshal(CategoriaImpl categoria) throws Exception {
        return categoria;
    }

    @Override
    public CategoriaImpl marshal(Categoria categoria) throws Exception {
        return (CategoriaImpl) categoria;
    }
}
