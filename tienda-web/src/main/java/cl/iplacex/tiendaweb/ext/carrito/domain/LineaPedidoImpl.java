package cl.iplacex.tiendaweb.ext.carrito.domain;

import cl.iplacex.tiendaweb.domain.Producto;
import cl.iplacex.tiendaweb.ext.carrito.adapter.ProductoAdapter;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class LineaPedidoImpl implements LineaPedido{
    @XmlJavaTypeAdapter(ProductoAdapter.class)
    private Producto producto;
    private int cantidad;

    public LineaPedidoImpl() {
    }

    public LineaPedidoImpl(Producto producto, int cantidad) {
        this.producto = producto;
        this.cantidad = cantidad;
    }

    @Override
    public Long getSubtotal() {
        return producto.getPrecioFinal() * cantidad;
    }

    @Override
    public Producto getProducto() {
        return producto;
    }

    @Override
    public int getCantidad() {
        return cantidad;
    }
}
