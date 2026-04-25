package cl.iplacex.tiendaweb.ext.carrito.domain;

import cl.iplacex.tiendaweb.domain.Producto;

public interface LineaPedido {
    Long getSubtotal();
    Producto getProducto();
    int getCantidad();
}
