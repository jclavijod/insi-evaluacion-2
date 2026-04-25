package cl.iplacex.tiendaweb.ext.carrito.observer;

import cl.iplacex.tiendaweb.ext.carrito.domain.LineaPedidoImpl;
import cl.iplacex.tiendaweb.ext.carrito.domain.Pedido;
import cl.iplacex.tiendaweb.ext.carrito.event.PedidoCompletadoEvent;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.StringWriter;

@Component
public class PedidoCompletadoListener {
    private final Logger logger = LoggerFactory.getLogger(PedidoCompletadoListener.class);

    @EventListener
    public void gestionarPedidoCompletado(PedidoCompletadoEvent event) {
        var pedido = event.getPedido();
        try {
            JAXBContext context = JAXBContext.newInstance(Pedido.class, LineaPedidoImpl.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            StringWriter writer = new StringWriter();
            marshaller.marshal(pedido, writer);
            var xml = writer.toString();
            logger.info("Pedido recibido como evento: {}", xml);
        } catch (JAXBException e) {
            logger.error("Error al convertir a XML", e);
            throw new RuntimeException(e);
        }
    }
}
