package cl.iplacex.tiendaweb.ext.carrito.domain;

import cl.iplacex.tiendaweb.ext.carrito.adapter.LocalDateTimeAdapter;
import jakarta.xml.bind.annotation.*;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "pedido")
public class Pedido {
    @XmlJavaTypeAdapter(LocalDateTimeAdapter.class)
    private LocalDateTime fecha = LocalDateTime.now();
    @XmlElementWrapper(name = "items")
    @XmlElement(name = "item")
    private List<LineaPedidoImpl> items = new ArrayList<>();
    private Comprador comprador = new Comprador();
    private Direccion direccionDespacho = new Direccion();

    public Long getTotal() {
        return this.items.stream().mapToLong(LineaPedidoImpl::getSubtotal).sum();
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public List<LineaPedidoImpl> getItems() {
        return items;
    }

    public void setItems(List<LineaPedidoImpl> items) {
        this.items = items;
    }

    public Comprador getComprador() {
        return comprador;
    }

    public void setComprador(Comprador comprador) {
        this.comprador = comprador;
    }

    public Direccion getDireccionDespacho() {
        return direccionDespacho;
    }

    public void setDireccionDespacho(Direccion direccionDespacho) {
        this.direccionDespacho = direccionDespacho;
    }
}
