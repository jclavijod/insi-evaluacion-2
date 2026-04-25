package cl.iplacex.tiendaweb.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
public class JmsProducer {

    private final JmsTemplate jmsTemplate;

    @Value("${queue.web.pedidos}")
    private String destinationQueue;

    public JmsProducer(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    public void enviarPedido(String xmlPayload) {
        // convertAndSend usa el MessageConverter por defecto; para String -> TextMessage está bien.
        jmsTemplate.convertAndSend(destinationQueue, xmlPayload);
    }
}