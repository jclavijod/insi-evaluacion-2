package cl.iplacex.tiendaweb.integration;

import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;

import cl.iplacex.tiendaweb.ext.carrito.domain.Pedido;
import cl.iplacex.tiendaweb.ext.carrito.event.PedidoCompletadoEvent;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import java.io.StringWriter;

@Component
public class PedidoEventListener {

    @Autowired
    private JmsTemplate jmsTemplate;

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

            // Enviar XML a la cola configurada (cambia el nombre si corresponde)
            jmsTemplate.convertAndSend("jpe_web_pedidos", xmlPedido);

            System.out.println("Pedido enviado a jpe_web_pedidos:\n" + xmlPedido);

        } catch (Exception e) {
            // Mejor manejar/loggear el error de forma apropiada en producción
            e.printStackTrace();
        }
    }
}