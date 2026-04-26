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
    private static final String OUT_CANONICAL = "jcl_canonical_pedidos";

    public static void main(String[] args) {
        ConnectionFactory cf = new ActiveMQConnectionFactory(BROKER_URL);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try (Connection connection = cf.createConnection()) {
            connection.start();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer producer = session.createProducer(session.createQueue(OUT_CANONICAL));

            System.out.println("[INFO] Traductor iniciado.");
            System.out.println("[INFO] Escuchando: " + IN_WEB + "  y " + IN_MKP);
            System.out.println("[INFO] Enviando a canónica: " + OUT_CANONICAL);

            // Thread para WebStore (XML -> canónico)
            new Thread(() -> {
                try {
                    MessageConsumer consumer = session.createConsumer(session.createQueue(IN_WEB));
                    while (true) {
                        Message m = consumer.receive(1000);
                        if (m == null) continue;
                        if (!(m instanceof TextMessage)) continue;
                        String xml = ((TextMessage) m).getText();
                        System.out.println("\n[LOG 3] Transformando XML (WebStore):\n" + xml);

                        JsonObject c = new JsonObject();
                        c.addProperty("idVenta", "WEB-" + System.currentTimeMillis());
                        c.addProperty("origen", "TIENDA_WEB");
                        c.addProperty("fechaHora", java.time.Instant.now().toString());
                        c.addProperty("rawXml", xml);

                        String payload = gson.toJson(c);
                        System.out.println("[OUT] JSON Canónico:\n" + payload);

                        producer.send(session.createTextMessage(payload));
                        System.out.println(">>> Enviado a " + OUT_CANONICAL);
                    }
                } catch (Exception e) {
                    System.err.println("[ERROR] Traductor Web:");
                    e.printStackTrace();
                }
            }, "translator-web-thread").start();

            // Thread para Marketplace (JSON -> canónico)
            new Thread(() -> {
                try {
                    MessageConsumer consumer = session.createConsumer(session.createQueue(IN_MKP));
                    while (true) {
                        Message m = consumer.receive(1000);
                        if (m == null) continue;
                        if (!(m instanceof TextMessage)) continue;
                        String json = ((TextMessage) m).getText();
                        System.out.println("\n[LOG 4] Transformando JSON (MKP):\n" + json);

                        JsonObject in = JsonParser.parseString(json).getAsJsonObject();
                        JsonObject c = new JsonObject();
                        c.addProperty("idVenta", in.has("id") ? in.get("id").getAsString() : "MKP-" + System.currentTimeMillis());
                        c.addProperty("origen", "MARKETPLACE");
                        c.addProperty("fechaHora", java.time.Instant.now().toString());
                        c.add("rawMarketplace", in);

                        String payload = gson.toJson(c);
                        System.out.println("[OUT] JSON Canónico:\n" + payload);

                        producer.send(session.createTextMessage(payload));
                        System.out.println(">>> Enviado a " + OUT_CANONICAL);
                    }
                } catch (Exception e) {
                    System.err.println("[ERROR] Traductor MKP:");
                    e.printStackTrace();
                }
            }, "translator-mkp-thread").start();

            // Mantener hilo principal vivo
            Thread.currentThread().join();
        } catch (Throwable e) {
            System.err.println("[FATAL] Traductores:");
            e.printStackTrace();
            System.exit(1);
        }
    }
}