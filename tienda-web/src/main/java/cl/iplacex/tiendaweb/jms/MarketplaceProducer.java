package cl.iplacex.tiendaweb.jms;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
public class MarketplaceProducer {

    private final JmsTemplate jms;
    private final String destino;

    public MarketplaceProducer(JmsTemplate jms,
                               @Value("${queue.marketplace.pedidos}") String destino) {
        this.jms = jms;
        this.destino = destino;
    }

    public void send(String json) {
        System.out.println("Enviando mensaje al destino: " + destino);
        jms.convertAndSend(destino, json);
    }
}