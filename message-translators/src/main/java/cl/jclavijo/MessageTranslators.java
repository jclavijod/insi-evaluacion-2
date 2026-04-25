package cl.jclavijo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * MessageTranslators
 *
 * Implementa los traductores para:
 * 1. XML (Web) -> JSON Canónico [LOG 3]
 * 2. JSON Enriquecido (MKP) -> JSON Canónico [LOG 4]
 *
 * Publica el resultado en la cola final: jcl_pedidos
 */
public class MessageTranslators {

    private static final String BROKER_URL = "tcp://localhost:61616";
    private static final String IN_WEB = "jcl_web_pedidos";
    private static final String IN_MKP = "jcl_mkp_pedidos";
    private static final String OUT_CANONICAL = "jcl_pedidos";

    public static void main(String[] args) {
        ConnectionFactory cf = new ActiveMQConnectionFactory(BROKER_URL);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try {
            Connection connection = cf.createConnection();
            connection.start();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            Destination targetQueue = session.createQueue(OUT_CANONICAL);
            MessageProducer producer = session.createProducer(targetQueue);

            System.out.println("[INFO] Traductores iniciados. Escuchando colas de entrada...");
            System.out.println("[INFO] WEB  -> " + IN_WEB);
            System.out.println("[INFO] MKP  -> " + IN_MKP);
            System.out.println("[INFO] OUT  -> " + OUT_CANONICAL);

            MessageConsumer webConsumer = session.createConsumer(session.createQueue(IN_WEB));
            webConsumer.setMessageListener(message -> {
                try {
                    if (message instanceof TextMessage textMsg) {
                        String xmlBody = textMsg.getText();
                        System.out.println("\n----");
                        System.out.println("[LOG 3] Transformando XML a JSON Canónico");
                        System.out.println("ENTRADA (XML): " + xmlBody);

                        JsonObject canonical = buildCanonicalFromXml(xmlBody);
                        String jsonOutput = gson.toJson(canonical);

                        System.out.println("SALIDA (JSON Canónico): " + jsonOutput);
                        producer.send(session.createTextMessage(jsonOutput));
                        System.out.println(">>> Enviado a cola canónica: " + OUT_CANONICAL);
                    }
                } catch (Exception e) {
                    System.err.println("[ERROR] Falló traductor XML -> JSON canónico");
                    e.printStackTrace();
                }
            });

            MessageConsumer mkpConsumer = session.createConsumer(session.createQueue(IN_MKP));
            mkpConsumer.setMessageListener(message -> {
                try {
                    if (message instanceof TextMessage textMsg) {
                        String rawJson = textMsg.getText();
                        System.out.println("\n----");
                        System.out.println("[LOG 4] Transformando JSON Enriquecido a JSON Canónico");
                        System.out.println("ENTRADA (JSON Enriquecido): " + rawJson);

                        JsonObject input = JsonParser.parseString(rawJson).getAsJsonObject();
                        JsonObject canonical = buildCanonicalFromMarketplace(input);

                        String jsonOutput = gson.toJson(canonical);
                        System.out.println("SALIDA (JSON Canónico): " + jsonOutput);

                        producer.send(session.createTextMessage(jsonOutput));
                        System.out.println(">>> Enviado a cola canónica: " + OUT_CANONICAL);
                    }
                } catch (Exception e) {
                    System.err.println("[ERROR] Falló traductor Marketplace -> JSON canónico");
                    e.printStackTrace();
                }
            });

            Thread.sleep(Long.MAX_VALUE);

        } catch (Exception e) {
            System.out.println("[ERROR] Error en Traductores:");
            e.printStackTrace();
        }
    }

    private static JsonObject buildCanonicalFromXml(String xmlBody) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(false);

        Document doc = dbf.newDocumentBuilder().parse(
                new ByteArrayInputStream(xmlBody.getBytes(StandardCharsets.UTF_8))
        );
        doc.getDocumentElement().normalize();

        String id = getTextContent(doc, "id");
        String cliente = getTextContent(doc, "cliente");
        String email = getTextContent(doc, "clienteEmail");
        String total = getTextContent(doc, "total");

        JsonObject canonical = new JsonObject();
        canonical.addProperty("idVenta", notBlank(id) ? id : "WEB-" + System.currentTimeMillis());
        canonical.addProperty("origen", "TIENDA_WEB");
        canonical.addProperty("fechaHora", Instant.now().toString());
        canonical.addProperty("estado", "PENDIENTE_FACTURACION");

        if (notBlank(total)) {
            try {
                canonical.addProperty("total", Double.parseDouble(total));
            } catch (NumberFormatException e) {
                canonical.addProperty("total", 0);
            }
        } else {
            canonical.addProperty("total", 0);
        }

        JsonObject clienteObj = new JsonObject();
        clienteObj.addProperty("nombre", notBlank(cliente) ? cliente : "Desconocido");
        if (notBlank(email)) {
            clienteObj.addProperty("email", email);
        }
        canonical.add("cliente", clienteObj);

        JsonArray detalle = new JsonArray();
        JsonObject item = new JsonObject();
        item.addProperty("codigo", notBlank(id) ? id : "WEB-ITEM");
        item.addProperty("cantidad", 1);
        detalle.add(item);
        canonical.add("detalle", detalle);

        return canonical;
    }

    private static JsonObject buildCanonicalFromMarketplace(JsonObject input) {
        JsonObject canonical = new JsonObject();

        String idVenta = getString(input, "idVenta");
        if (!notBlank(idVenta)) {
            idVenta = getString(input, "id");
        }
        if (!notBlank(idVenta)) {
            idVenta = "MKP-" + System.currentTimeMillis();
        }

        canonical.addProperty("idVenta", idVenta);
        canonical.addProperty("origen", "MARKETPLACE");
        canonical.addProperty("fechaHora", getString(input, "fechaHora") != null ? getString(input, "fechaHora") : Instant.now().toString());
        canonical.addProperty("estado", "PENDIENTE_FACTURACION");

        if (input.has("costoEnvio") && input.get("costoEnvio").isJsonPrimitive()) {
            try {
                canonical.addProperty("costoEnvio", input.get("costoEnvio").getAsDouble());
            } catch (Exception e) {
                canonical.addProperty("costoEnvio", 0);
            }
        } else {
            canonical.addProperty("costoEnvio", 0);
        }

        canonical.addProperty("moneda", getString(input, "monedaEnvio") != null ? getString(input, "monedaEnvio") : "CLP");

        JsonObject cliente = new JsonObject();
        cliente.addProperty("nombre", getNestedString(input, "cliente", "nombre") != null
                ? getNestedString(input, "cliente", "nombre")
                : (getString(input, "clienteNombre") != null ? getString(input, "clienteNombre") : "Desconocido"));
        String email = getNestedString(input, "cliente", "email");
        if (email == null) {
            email = getString(input, "clienteEmail");
        }
        if (notBlank(email)) {
            cliente.addProperty("email", email);
        }
        canonical.add("cliente", cliente);

        JsonArray detalle = new JsonArray();
        if (input.has("detalle") && input.get("detalle").isJsonArray()) {
            input.getAsJsonArray("detalle").forEach(node -> {
                if (node.isJsonObject()) {
                    JsonObject item = node.getAsJsonObject();
                    JsonObject canonItem = new JsonObject();
                    canonItem.addProperty("codigo", getString(item, "codigo") != null ? getString(item, "codigo") : "");
                    canonItem.addProperty("cantidad", getInt(item, "cantidad", 1));
                    detalle.add(canonItem);
                }
            });
        } else if (input.has("items") && input.get("items").isJsonArray()) {
            input.getAsJsonArray("items").forEach(node -> {
                if (node.isJsonObject()) {
                    JsonObject item = node.getAsJsonObject();
                    JsonObject canonItem = new JsonObject();
                    canonItem.addProperty("codigo", getString(item, "codigo") != null ? getString(item, "codigo") : "");
                    canonItem.addProperty("cantidad", getInt(item, "cantidad", 1));
                    detalle.add(canonItem);
                }
            });
        }

        if (detalle.size() == 0) {
            JsonObject item = new JsonObject();
            item.addProperty("codigo", "MKP-ITEM");
            item.addProperty("cantidad", 1);
            detalle.add(item);
        }

        canonical.add("detalle", detalle);

        return canonical;
    }

    private static String getTextContent(Document doc, String tagName) {
        NodeList nodes = doc.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            return null;
        }
        Node node = nodes.item(0);
        return node != null ? node.getTextContent().trim() : null;
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static String getString(JsonObject obj, String key) {
        try {
            if (obj != null && obj.has(key) && !obj.get(key).isJsonNull()) {
                return obj.get(key).getAsString().trim();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String getNestedString(JsonObject obj, String parent, String child) {
        try {
            if (obj != null && obj.has(parent) && obj.get(parent).isJsonObject()) {
                JsonObject nested = obj.getAsJsonObject(parent);
                return getString(nested, child);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static int getInt(JsonObject obj, String key, int defaultValue) {
        try {
            if (obj != null && obj.has(key) && !obj.get(key).isJsonNull()) {
                return obj.get(key).getAsInt();
            }
        } catch (Exception ignored) {
        }
        return defaultValue;
    }
}