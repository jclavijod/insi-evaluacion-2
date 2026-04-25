package cl.iplacex.tiendaweb.jms;

import cl.iplacex.tiendaweb.model.PedidoCanonico;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class OrderTranslatorListener {

    private final JmsTemplate jmsTemplate;
    private final ObjectMapper objectMapper;

    // cola de salida (JSON canónico)
    private final String colaCanonical;

    public OrderTranslatorListener(JmsTemplate jmsTemplate,
                                   ObjectMapper objectMapper,
                                   @Value("${queue.canonical.pedidos}") String colaCanonical) {
        this.jmsTemplate = jmsTemplate;
        this.objectMapper = objectMapper;
        this.colaCanonical = colaCanonical;
    }

    @JmsListener(destination = "${queue.web.pedidos}")
    public void receiveMessage(String xmlMessage) {
        try {
            System.out.println(">>> Traductor recibió XML (long=" + (xmlMessage != null ? xmlMessage.length() : 0) + "): " + (xmlMessage == null ? "null" : xmlMessage.replaceAll("\\r?\\n", "")));

            // Parseo sencillo del XML usando DOM (suficiente para ejemplos)
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(false);
            var db = dbf.newDocumentBuilder();
            Document doc = db.parse(new ByteArrayInputStream(xmlMessage.getBytes(StandardCharsets.UTF_8)));
            doc.getDocumentElement().normalize();

            PedidoCanonico pedido = new PedidoCanonico();
            pedido.idVenta = getTextContent(doc, "/pedido/id");
            if (pedido.idVenta == null || pedido.idVenta.isBlank()) {
                // generar id si no viene
                pedido.idVenta = "WEB-" + Instant.now().toEpochMilli();
            }
            pedido.origen = "TIENDA_WEB";
            pedido.fechaHora = Instant.now().toString();

            // Cliente
            PedidoCanonico.Cliente cliente = new PedidoCanonico.Cliente();
            cliente.nombre = getTextContent(doc, "/pedido/cliente");
            cliente.email = getTextContent(doc, "/pedido/clienteEmail"); // si no existe, queda null
            pedido.cliente = cliente;

            // Items (busca nodos /pedido/items/item o /pedido/item)
            List<PedidoCanonico.Item> items = new ArrayList<>();
            NodeList itemNodes = doc.getElementsByTagName("item");
            for (int i = 0; i < itemNodes.getLength(); i++) {
                Node it = itemNodes.item(i);
                if (it.getNodeType() == Node.ELEMENT_NODE) {
                    Element e = (Element) it;
                    PedidoCanonico.Item pi = new PedidoCanonico.Item();
                    String codigo = getChildText(e, "codigo");
                    String cantidadStr = getChildText(e, "cantidad");
                    pi.codigo = codigo != null ? codigo : "";
                    try {
                        pi.cantidad = cantidadStr != null ? Integer.parseInt(cantidadStr) : 1;
                    } catch (NumberFormatException ex) {
                        pi.cantidad = 1;
                    }
                    items.add(pi);
                }
            }
            pedido.detalle = items;

            // Serializar a JSON canónico
            String jsonCanonico = objectMapper.writeValueAsString(pedido);

            System.out.println("<<< Traductor produjo JSON canónico: " + jsonCanonico);

            // Enviar JSON canónico a la cola siguiente
            jmsTemplate.convertAndSend(colaCanonical, jsonCanonico);
            System.out.println(">>> Enviado a cola canonical: " + colaCanonical);

        } catch (Exception e) {
            System.err.println("Error traduciendo mensaje XML -> JSON: " + e.getMessage());
            e.printStackTrace();
            // Aquí podrías enviar el mensaje a una DLQ o a una cola de errores
        }
    }

    // Helpers para extraer texto de XML
    private static String getTextContent(Document doc, String xpathLike) {
        // xpathLike simple: /pedido/cliente
        String[] parts = xpathLike.split("/");
        Node node = doc.getDocumentElement();
        for (int i = 1; i < parts.length; i++) {
            if (node == null) return null;
            String tag = parts[i];
            NodeList children = node.getChildNodes();
            Node next = null;
            for (int j = 0; j < children.getLength(); j++) {
                Node c = children.item(j);
                if (c.getNodeType() == Node.ELEMENT_NODE && c.getNodeName().equals(tag)) {
                    next = c;
                    break;
                }
            }
            node = next;
        }
        return node != null ? node.getTextContent().trim() : null;
    }

    private static String getChildText(Element parent, String childName) {
        NodeList nl = parent.getElementsByTagName(childName);
        if (nl.getLength() == 0) return null;
        return nl.item(0).getTextContent().trim();
    }
}