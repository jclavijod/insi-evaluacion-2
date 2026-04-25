package cl.iplacex.tiendaweb.jms;

import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class WarehouseListener {

    @JmsListener(destination = "${queue.bodega}")
    public void consumirPedidoBodega(String mensaje) {
        System.out.println("[LOG 6] Pedido consumido por Bodega:");
        System.out.println(mensaje);
    }
}