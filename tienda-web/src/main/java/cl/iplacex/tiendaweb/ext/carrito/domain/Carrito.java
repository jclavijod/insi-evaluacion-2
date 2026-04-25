package cl.iplacex.tiendaweb.ext.carrito.domain;

import cl.iplacex.tiendaweb.domain.Producto;

import java.util.List;

public interface Carrito {
    List<LineaPedido> getLineasPedido();
    void agregarProducto(Producto producto, int cantidad);
    void quitarProducto(String sku);
    Long getTotal();
    int getCantidad();
}
