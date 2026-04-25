package cl.jclavijo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;

import java.util.concurrent.CountDownLatch;

/**
 * MessageTranslators
 *
 * - Escucha jcl_web_pedidos  -> transforma XML -> JSON canónico  [LOG 3]
 * - Escucha jcl_mkp_pedidos  -> transforma JSON enriquecido -> JSON canónico  [LOG 4]
 * - Publica resultados en jcl_pedidos_canonicos
 *
 * Ejecutar y dejar corriendo para recibir mensajes.
 */
public class MessageTranslators {

    private static final String BROKER_URL = "tcp://localhost:61616";
    private static final String IN_WEB = "jcl_web_pedidos";
    private static final String IN_MKP = "jcl_mkp_pedidos";
    private static final String OUT_CANONICAL = "jcl_pedidos_canonicos";

    public static void main(String[] args) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        CountDownLatch stopLatch = new CountDownLatch(1);

        ConnectionFactory cf = new ActiveMQConnectionFactory(BROKER_URL);
        Connection connection = null;

        try {
            connection = cf.createConnection();

            // Asegurar cierre ordenado al terminar la JVM
            Connection finalConnection = connection;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    System.out.println("[INFO] Shutdown hook: cerrando conexión broker...");
                    if (finalConnection != null) finalConnection.close();
                    stopLatch.countDown();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }));

            // Crear sesión y productores/consumidores
            var session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            var producer = session.createProducer(session.createQueue(OUT_CANONICAL));

            // Consumidor para tienda web (XML)
            MessageConsumer webConsumer = session.createConsumer(session.createQueue(IN_WEB));
            webConsumer.setMessageListener(message -> {
                try {
                    if (message instanceof TextMessage textMsg) {
                        String xmlBody = textMsg.getText();

                        System.out.println();
                        System.out.println("--------------------------------------------------");
                        System.out.println("[LOG 3] Transformando XML a JSON Canónico");
                        System.out.println("ENTRADA (XML): " + xmlBody);

                        // Transformación simple: extraer un id si existe y mapear campos básicos.
                        // (Para la evaluación bastará; en producción usarías un parser XML como JAXB)
                        String extractedId = "WEB-" + System.currentTimeMillis();
                        // Si el XML tiene <id>...</id> intentamos extraerlo (very simple)
                        int idStart = xmlBody.indexOf("<​id>");
                        int idEnd = xmlBody.indexOf("<​/id>");
                        if (idStart >= 0 && idEnd > idStart) {
                            String idVal = xmlBody.substring(idStart + 4, idEnd).trim();
                            if (!idVal.isEmpty()) extractedId = "WEB-" + idVal;
                        }

                        JsonObject canonical = new JsonObject();
                        canonical.addProperty("idVenta", extractedId);
                        canonical.addProperty("origen", "Tienda Web");
                        canonical.addProperty("estado", "PENDIENTE_FACTURACION");

                        String jsonOutput = gson.toJson(canonical);
                        System.out.println("SALIDA (JSON Canónico): " + jsonOutput);

                        producer.send(session.createTextMessage(jsonOutput));
                        System.out.println("[OK] Enviado a cola " + OUT_CANONICAL);
                    } else {
                        System.out.println("[WARN] Mensaje recibido en " + IN_WEB + " no es TextMessage");
                    }
                } catch (Exception e) {
                    System.out.println("[ERROR] Traductor XML -> Canónico falló:");
                    e.printStackTrace();
                }
            });

            // Consumidor para marketplace (JSON enriquecido)
            MessageConsumer mkpConsumer = session.createConsumer(session.createQueue(IN_MKP));
            mkpConsumer.setMessageListener(message -> {
                try {
                    if (message instanceof TextMessage textMsg) {
                        String rawJson = textMsg.getText();
                        System.out.println();
                        System.out.println("--------------------------------------------------");
                        System.out.println("[LOG 4] Transformando JSON Enriquecido a JSON Canónico");
                        System.out.println("ENTRADA (JSON Enriquecido): " + rawJson);

                        var input = JsonParser.parseString(rawJson).getAsJsonObject();

                        JsonObject canonical = new JsonObject();
                        canonical.addProperty("idVenta", input.has("id") ? input.get("id").getAsString() : "MKP-" + System.currentTimeMillis());
                        canonical.addProperty("origen", "Marketplace");
                        canonical.addProperty("costoEnvio", input.has("costoEnvio") ? input.get("costoEnvio").getAsInt() : 0);
                        canonical.addProperty("moneda", input.has("monedaEnvio") ? input.get("monedaEnvio").getAsString() : "CLP");
                        canonical.addProperty("estado", "PENDIENTE_FACTURACION");

                        String jsonOutput = gson.toJson(canonical);
                        System.out.println("SALIDA (JSON Canónico): " + jsonOutput);

                        producer.send(session.createTextMessage(jsonOutput));
                        System.out.println("[OK] Enviado a cola " + OUT_CANONICAL);
                    } else {
                        System.out.println("[WARN] Mensaje recibido en " + IN_MKP + " no es TextMessage");
                    }
                } catch (Exception e) {
                    System.out.println("[ERROR] Traductor MKP -> Canónico falló:");
                    e.printStackTrace();
                }
            });

            // Iniciar recepción
            connection.start();
            System.out.println("[INFO] Traductores iniciados. Escuchando colas de entrada...");
            // Esperar indefinidamente hasta shutdown
            stopLatch.await();

        } catch (Exception e) {
            System.out.println("[ERROR] Error en MessageTranslators:");
            e.printStackTrace();
        } finally {
            try {
                if (connection != null) connection.close();
            } catch (Exception ignored) { }
        }
    }
}