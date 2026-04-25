package cl.iplacex.tiendaweb.ext.carrito.domain;

import cl.iplacex.tiendaweb.domain.Producto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CarritoImpl implements Carrito {
    private final Map<String, LineaPedido> carro = new HashMap<>();

    @Override
    public List<LineaPedido> getLineasPedido() {
        return carro.values().stream().toList();
    }

    @Override
    public void agregarProducto(Producto producto, int cantidad) {
        carro.put(producto.getSku(), new LineaPedidoImpl(producto, cantidad));
    }

    @Override
    public void quitarProducto(String sku) {
        carro.remove(sku);
    }

    @Override
    public Long getTotal() {
        return carro.values().stream().mapToLong(LineaPedido::getSubtotal).sum();
    }

    public int getCantidad() {
        return carro.values().stream().mapToInt(LineaPedido::getCantidad).sum();
    }
}
