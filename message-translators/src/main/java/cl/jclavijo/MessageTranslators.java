package cl.jclavijo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.jms.*;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;

public class MessageTranslators {
    private static final String BROKER_URL = "tcp://localhost:61616";
    private static final String IN_WEB = "jcl_web_pedidos";
    private static final String IN_MKP = "jcl_mkp_pedidos";
    private static final String OUT_CANONICAL = "jcl_pedidos";

    public static void main(String[] args) {
        ConnectionFactory cf = new ActiveMQConnectionFactory(BROKER_URL);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try (Connection connection = cf.createConnection()) {
            connection.start();
            Session session = connection.createSession(Session.AUTO_ACKNOWLEDGE);
            MessageProducer producer = session.createProducer(session.createQueue(OUT_CANONICAL));

            System.out.println("[INFO] Traductores iniciados. Destino: " + OUT_CANONICAL);

            // Traductor WEB
            session.createConsumer(session.createQueue(IN_WEB)).setMessageListener(m -> {
                try {
                    String xml = ((TextMessage)m).getText();
                    System.out.println("\n[LOG 3] Transformando XML: " + xml);
                    JsonObject c = new JsonObject();
                    c.addProperty("idVenta", "WEB-" + System.currentTimeMillis());
                    c.addProperty("origen", "TIENDA_WEB");
                    producer.send(session.createTextMessage(gson.toJson(c)));
                } catch (Exception e) { e.printStackTrace(); }
            });

            // Traductor MKP
            session.createConsumer(session.createQueue(IN_MKP)).setMessageListener(m -> {
                try {
                    String json = ((TextMessage)m).getText();
                    System.out.println("\n[LOG 4] Transformando JSON MKP: " + json);
                    JsonObject in = JsonParser.parseString(json).getAsJsonObject();
                    JsonObject c = new JsonObject();
                    c.addProperty("idVenta", in.has("id") ? in.get("id").getAsString() : "MKP-UNK");
                    c.addProperty("origen", "MARKETPLACE");
                    producer.send(session.createTextMessage(gson.toJson(c)));
                } catch (Exception e) { e.printStackTrace(); }
            });

            Thread.sleep(Long.MAX_VALUE);
        } catch (Exception e) { e.printStackTrace(); }
    }
}