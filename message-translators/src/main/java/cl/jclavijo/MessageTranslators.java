package cl.jclavijo;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.jms.*;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;

public class MessageTranslators {
    private static final String BROKER_URL = "tcp://localhost:61616";
    private static final String IN_WEB = "jcl_web_pedidos";    // Cola entrada XML
    private static final String IN_MKP = "jcl_mkp_pedidos";    // Cola entrada JSON enriquecido
    private static final String OUT_CANONICAL = "jcl_pedidos_canonicos"; // Cola salida única

    public static void main(String[] args) throws Exception {
        ConnectionFactory cf = new ActiveMQConnectionFactory(BROKER_URL);
        Connection conn = cf.createConnection();
        Session session = conn.createSession(Session.AUTO_ACKNOWLEDGE);
        conn.start();

        Gson gson = new Gson();
        System.out.println("[INFO] Traductores iniciados. Esperando mensajes...");

        // --- TRADUCTOR 1: XML (Tienda Web) -> JSON Canónico ---
        MessageConsumer webConsumer = session.createConsumer(session.createQueue(IN_WEB));
        webConsumer.setMessageListener(msg -> {
            try {
                String xmlBody = ((TextMessage) msg).getText();
                System.out.println("\n[LOG 3] Transformando XML a JSON Canónico");
                System.out.println("ENTRADA (XML): " + xmlBody);
                
                // Simulación de transformación XML a nuestro JSON Canónico
                JsonObject canonical = new JsonObject();
                canonical.addProperty("idVenta", "WEB-" + System.currentTimeMillis());
                canonical.addProperty("origen", "Tienda Web");
                // Aquí iría el parseo real del XML...
                
                System.out.println("SALIDA (JSON Canónico): " + gson.toJson(canonical));
            } catch (Exception e) { e.printStackTrace(); }
        });

        // --- TRADUCTOR 2: JSON Enriquecido (Marketplace) -> JSON Canónico ---
        MessageConsumer mkpConsumer = session.createConsumer(session.createQueue(IN_MKP));
        mkpConsumer.setMessageListener(msg -> {
            try {
                String rawJson = ((TextMessage) msg).getText();
                JsonObject input = JsonParser.parseString(rawJson).getAsJsonObject();
                
                System.out.println("\n[LOG 4] Transformando JSON Enriquecido a JSON Canónico");
                System.out.println("ENTRADA (JSON Enriquecido): " + rawJson);

                // Mapeo al formato canónico
                JsonObject canonical = new JsonObject();
                canonical.addProperty("idVenta", input.get("id").getAsString());
                canonical.addProperty("origen", "Marketplace");
                canonical.addProperty("costoEnvio", input.get("costoEnvio").getAsInt());
                
                System.out.println("SALIDA (JSON Canónico): " + gson.toJson(canonical));
            } catch (Exception e) { e.printStackTrace(); }
        });
    }
}