package cl.jclavijo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.jms.*;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;

/**
 * MessageTranslators
 * 
 * Implementa los traductores para:
 * 1. XML (Web) -> JSON Canónico [LOG 3]
 * 2. JSON Enriquecido (MKP) -> JSON Canónico [LOG 4]
 * 
 * Publica el resultado en la cola final: jcl_pedidos_canonicos
 */
public class MessageTranslators {

    private static final String BROKER_URL = "tcp://localhost:61616";
    private static final String IN_WEB = "jcl_web_pedidos";    // Entrada XML
    private static final String IN_MKP = "jcl_mkp_pedidos";    // Entrada JSON Enriquecido
    private static final String OUT_CANONICAL = "jcl_pedidos_canonicos"; // Destino final

    public static void main(String[] args) {
        ConnectionFactory cf = new ActiveMQConnectionFactory(BROKER_URL);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try {
            Connection connection = cf.createConnection();
            connection.start();
            Session session = connection.createSession(Session.AUTO_ACKNOWLEDGE);
            
            Destination targetQueue = session.createQueue(OUT_CANONICAL);
            MessageProducer producer = session.createProducer(targetQueue);

            System.out.println("[INFO] Traductores iniciados. Escuchando colas de entrada...");

            // --- TRADUCTOR 1: XML -> JSON CANÓNICO [LOG 3] ---
            MessageConsumer webConsumer = session.createConsumer(session.createQueue(IN_WEB));
            webConsumer.setMessageListener(message -> {
                try {
                    if (message instanceof TextMessage textMsg) {
                        String xmlBody = textMsg.getText();
                        System.out.println("\n--------------------------------------------------");
                        System.out.println("[LOG 3] Transformando XML a JSON Canónico");
                        System.out.println("ENTRADA (XML): " + xmlBody);

                        // Simulación de transformación XML (o extracción simple)
                        JsonObject canonical = new JsonObject();
                        canonical.addProperty("idVenta", "WEB-" + System.currentTimeMillis());
                        canonical.addProperty("origen", "Tienda Web");
                        canonical.addProperty("estado", "PENDIENTE_FACTURACION");
                        
                        String jsonOutput = gson.toJson(canonical);
                        System.out.println("SALIDA (JSON Canónico): " + jsonOutput);

                        producer.send(session.createTextMessage(jsonOutput));
                    }
                } catch (Exception e) { e.printStackTrace(); }
            });

            // --- TRADUCTOR 2: JSON MKP -> JSON CANÓNICO [LOG 4] ---
            MessageConsumer mkpConsumer = session.createConsumer(session.createQueue(IN_MKP));
            mkpConsumer.setMessageListener(message -> {
                try {
                    if (message instanceof TextMessage textMsg) {
                        String rawJson = textMsg.getText();
                        JsonObject input = JsonParser.parseString(rawJson).getAsJsonObject();
                        
                        System.out.println("\n--------------------------------------------------");
                        System.out.println("[LOG 4] Transformando JSON Enriquecido a JSON Canónico");
                        System.out.println("ENTRADA (JSON Enriquecido): " + rawJson);

                        // Mapeo al modelo canónico que definimos
                        JsonObject canonical = new JsonObject();
                        canonical.addProperty("idVenta", input.has("id") ? input.get("id").getAsString() : "MKP-UNK");
                        canonical.addProperty("origen", "Marketplace");
                        canonical.addProperty("costoEnvio", input.has("costoEnvio") ? input.get("costoEnvio").getAsInt() : 0);
                        canonical.addProperty("moneda", input.has("monedaEnvio") ? input.get("monedaEnvio").getAsString() : "CLP");
                        
                        String jsonOutput = gson.toJson(canonical);
                        System.out.println("SALIDA (JSON Canónico): " + jsonOutput);

                        producer.send(session.createTextMessage(jsonOutput));
                    }
                } catch (Exception e) { e.printStackTrace(); }
            });

            // Mantener la app corriendo
            Thread.sleep(Long.MAX_VALUE);

        } catch (Exception e) {
            System.out.println("[ERROR] Error en Traductores:");
            e.printStackTrace();
        }
    }
}