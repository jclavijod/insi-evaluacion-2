package cl.iplacex.tiendaweb.jms;

import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class WarehouseAdapterListener {

    @JmsListener(destination = "${queue.bodega.pedidos:jcl_bodega}")
    public void procesarParaBodega(String jsonCanonico) {
        try {
            System.out.println(">>> BODEGA (Warehouse) recibió pedido canónico para despacho:");
            System.out.println(jsonCanonico);

            // Simulación de lógica de bodega
            System.out.println(">>> BODEGA: Procesando stock y preparando envío... OK");

        } catch (Exception e) {
            System.err.println("Error en Warehouse Adapter: " + e.getMessage());
            e.printStackTrace();
        }
    }
}