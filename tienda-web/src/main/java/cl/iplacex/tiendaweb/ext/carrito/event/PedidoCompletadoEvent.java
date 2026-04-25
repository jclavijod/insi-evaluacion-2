package cl.iplacex.tiendaweb.ext.carrito.event;

import cl.iplacex.tiendaweb.ext.carrito.domain.Pedido;

public class PedidoCompletadoEvent {

    private final Pedido pedido;

    public PedidoCompletadoEvent(Pedido pedido) {
        this.pedido = pedido;
    }

    public Pedido getPedido() {
        return pedido;
    }
}
