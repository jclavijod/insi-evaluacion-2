package cl.iplacex.tiendaweb.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import cl.iplacex.tiendaweb.ext.carrito.domain.Pedido;
import cl.iplacex.tiendaweb.ext.carrito.event.PedidoCompletadoEvent;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import java.io.StringWriter;

@Component
public class PedidoEventListener {

    private final JmsTemplate jmsTemplate;
    private final String queueWebPedidos;

    public PedidoEventListener(JmsTemplate jmsTemplate,
                               @Value("${queue.web.pedidos}") String queueWebPedidos) {
        this.jmsTemplate = jmsTemplate;
        this.queueWebPedidos = queueWebPedidos;
    }

    @EventListener
    public void onPedidoCompletado(PedidoCompletadoEvent event) {
        try {
            // Usar el getter explícito del evento
            Pedido pedido = event.getPedido();

            // Marshall a XML con JAXB
            JAXBContext context = JAXBContext.newInstance(Pedido.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            StringWriter writer = new StringWriter();
            marshaller.marshal(pedido, writer);
            String xmlPedido = writer.toString();

            // Enviar XML a la cola configurada
            jmsTemplate.convertAndSend(queueWebPedidos, xmlPedido);
            System.out.println("Pedido enviado a cola [" + queueWebPedidos + "]:\n" + xmlPedido);
        } catch (Exception e) {
            System.err.println("Error serializando/enviando pedido: " + e.getMessage());
            e.printStackTrace();
        }
    }
}