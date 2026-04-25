package cl.iplacex.tiendaweb.ext.carrito.adapter;

import cl.iplacex.tiendaweb.domain.Producto;
import cl.iplacex.tiendaweb.domain.ProductoImpl;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;

public class ProductoAdapter extends XmlAdapter<ProductoImpl, Producto> {

    @Override
    public Producto unmarshal(ProductoImpl producto) throws Exception {
        return producto;
    }

    @Override
    public ProductoImpl marshal(Producto producto) throws Exception {
        return (ProductoImpl) producto;
    }
}
