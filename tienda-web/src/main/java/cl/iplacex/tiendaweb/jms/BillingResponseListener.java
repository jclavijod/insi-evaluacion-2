package cl.iplacex.tiendaweb.jms;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
public class BillingResponseListener {
    private final JmsTemplate jms;
    @Value("${queue.bodega}") private String colaBodega;

    public BillingResponseListener(JmsTemplate jms) { this.jms = jms; }

    @JmsListener(destination = "${queue.facturacion.respuesta}")
    public void onResponse(String ack) {
        System.out.println("[LOG 5] ACK Facturacion: " + ack);
        System.out.println(">>> Reenviando a Bodega...");
        jms.convertAndSend(colaBodega, ack);
    }
}