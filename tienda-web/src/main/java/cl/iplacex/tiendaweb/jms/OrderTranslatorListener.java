package cl.iplacex.tiendaweb.jms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
public class OrderTranslatorListener {

    private final JmsTemplate jmsTemplate;
    private final ObjectMapper objectMapper;
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
            System.out.println(">>> Traductor recibió XML (long=" + (xmlMessage != null ? xmlMessage.length() : 0) + "): "
                    + (xmlMessage == null ? "null" : xmlMessage.replaceAll("\\r?\\n", "")));

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(false);
            var db = dbf.newDocumentBuilder();
            Document doc = db.parse(new ByteArrayInputStream(xmlMessage.getBytes(StandardCharsets.UTF_8)));
            doc.getDocumentElement().normalize();

            ObjectNode pedido = objectMapper.createObjectNode();

            String id = getTextContent(doc, "/pedido/id");
            if (id == null || id.isBlank()) {
                id = "WEB-" + Instant.now().toEpochMilli();
            }
            pedido.put("idVenta", id);
            pedido.put("origen", "TIENDA_WEB");
            pedido.put("fechaHora", Instant.now().toString());

            ObjectNode cliente = objectMapper.createObjectNode();
            putText(cliente, "nombre", getNestedChildText(doc.getDocumentElement(), "comprador", "nombre"));
            putText(cliente, "rut", getNestedChildText(doc.getDocumentElement(), "comprador", "rut"));
            putText(cliente, "telefono", getNestedChildText(doc.getDocumentElement(), "comprador", "telefono"));
            putText(cliente, "email", getNestedChildText(doc.getDocumentElement(), "comprador", "email"));
            pedido.set("cliente", cliente);

            ObjectNode entrega = objectMapper.createObjectNode();
            putText(entrega, "calleYNumero", getNestedChildText(doc.getDocumentElement(), "direccionDespacho", "calleYNumero"));
            putText(entrega, "comuna", getNestedChildText(doc.getDocumentElement(), "direccionDespacho", "comuna"));
            pedido.set("entrega", entrega);

            ArrayNode detalle = objectMapper.createArrayNode();
            NodeList itemNodes = doc.getElementsByTagName("item");

            for (int i = 0; i < itemNodes.getLength(); i++) {
                Node it = itemNodes.item(i);
                if (it.getNodeType() == Node.ELEMENT_NODE) {
                    Element e = (Element) it;
                    ObjectNode linea = objectMapper.createObjectNode();

                    String sku = getNestedChildText(e, "producto", "sku");
                    String nombreProducto = getNestedChildText(e, "producto", "nombre");
                    String costoStr = getNestedChildText(e, "producto", "costo");
                    String precioListaStr = getNestedChildText(e, "producto", "precioLista");
                    String categoriaNombre = getNestedChildText(e, "producto", "categoria", "nombre");
                    String cantidadStr = getChildText(e, "cantidad");

                    putText(linea, "codigo", sku);
                    putText(linea, "nombreProducto", nombreProducto);
                    putText(linea, "categoria", categoriaNombre);

                    linea.put("cantidad", parseIntSafe(cantidadStr, 1));
                    linea.put("costo", parseLongSafe(costoStr, 0L));
                    linea.put("precioLista", parseLongSafe(precioListaStr, 0L));

                    detalle.add(linea);
                }
            }

            pedido.set("detalle", detalle);

            String jsonCanonico = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(pedido);
            System.out.println("<<< Traductor produjo JSON canónico:\n" + jsonCanonico);

            jmsTemplate.convertAndSend(colaCanonical, jsonCanonico);
            System.out.println(">>> Enviado a cola canonical: " + colaCanonical);

        } catch (Exception e) {
            System.err.println("Error traduciendo mensaje XML -> JSON: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void putText(ObjectNode node, String fieldName, String value) {
        if (value == null || value.isBlank()) {
            node.putNull(fieldName);
        } else {
            node.put(fieldName, value);
        }
    }

    private static int parseIntSafe(String value, int defaultValue) {
        try {
            return (value == null || value.isBlank()) ? defaultValue : Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static long parseLongSafe(String value, long defaultValue) {
        try {
            return (value == null || value.isBlank()) ? defaultValue : Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    // Helper: extrae texto vía un pseudo-xpath simple "/pedido/comprador/nombre"
    private static String getTextContent(Document doc, String xpathLike) {
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

    // Helper: obtiene texto directo de un hijo
    private static String getChildText(Element parent, String childName) {
        NodeList nl = parent.getElementsByTagName(childName);
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if (n.getParentNode() != null && n.getParentNode().isSameNode(parent)) {
                return n.getTextContent().trim();
            }
        }
        if (nl.getLength() > 0) {
            return nl.item(0).getTextContent().trim();
        }
        return null;
    }

    // Helper: obtiene texto de una ruta anidada dentro de un elemento
    private static String getNestedChildText(Node start, String... path) {
        Node current = start;
        for (String p : path) {
            if (current == null || current.getNodeType() != Node.ELEMENT_NODE) return null;
            Element currElem = (Element) current;
            NodeList children = currElem.getElementsByTagName(p);
            Node next = null;

            for (int i = 0; i < children.getLength(); i++) {
                Node candidate = children.item(i);
                if (candidate.getParentNode() != null && candidate.getParentNode().isSameNode(currElem)) {
                    next = candidate;
                    break;
                }
            }

            if (next == null && children.getLength() > 0) {
                next = children.item(0);
            }
            current = next;
        }
        return (current != null) ? current.getTextContent().trim() : null;
    }
}