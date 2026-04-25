package cl.jclavijo.facturacion;

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

import java.time.Instant;

public class FacturacionAdapterMain {

    private static final String BROKER_URL = "tcp://localhost:61616";
    private static final String IN_FACTURACION = "jcl_facturacion";
    private static final String OUT_RESPUESTA = "jcl_facturacion_respuesta";

    public static void main(String[] args) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        ConnectionFactory cf = new ActiveMQConnectionFactory(BROKER_URL);

        try {
            Connection connection = cf.createConnection();
            connection.start();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            MessageConsumer consumer = session.createConsumer(session.createQueue(IN_FACTURACION));
            MessageProducer producer = session.createProducer(session.createQueue(OUT_RESPUESTA));

            System.out.println("[INFO] Facturación legacy iniciada.");
            System.out.println("[INFO] Escuchando cola: " + IN_FACTURACION);

            consumer.setMessageListener(message -> {
                try {
                    if (message instanceof TextMessage textMessage) {
                        String soap = textMessage.getText();
                        System.out.println("\n----");
                        System.out.println("[LOG 5] SOAP recibido por el sistema de facturación legado:");
                        System.out.println(soap);

                        String idVenta = extractSoapValue(soap, "idVenta");
                        String origen = extractSoapValue(soap, "origen");

                        JsonObject ack = new JsonObject();
                        ack.addProperty("idVenta", idVenta != null ? idVenta : "N/A");
                        ack.addProperty("origen", origen != null ? origen : "TIENDA_WEB");
                        ack.addProperty("status", "OK");
                        ack.addProperty("mensaje", "Pedido registrado correctamente en facturación");
                        ack.addProperty("fechaHora", Instant.now().toString());

                        String ackJson = gson.toJson(ack);
                        producer.send(session.createTextMessage(ackJson));

                        System.out.println(">>> ACK enviado a cola: " + OUT_RESPUESTA);
                        System.out.println(ackJson);
                    }
                } catch (Exception e) {
                    System.err.println("Error en facturación legado:");
                    e.printStackTrace();
                }
            });

            Thread.sleep(Long.MAX_VALUE);

        } catch (Exception e) {
            System.err.println("[ERROR] Facturación legacy no pudo iniciar:");
            e.printStackTrace();
        }
    }

    private static String extractSoapValue(String soap, String tag) {
        try {
            String open = "<​fac:" + tag + ">";
            String close = "<​/fac:" + tag + ">";
            int start = soap.indexOf(open);
            if (start < 0) {
                open = "<​" + tag + ">";
                close = "<​/" + tag + ">";
                start = soap.indexOf(open);
            }
            if (start < 0) return null;

            start += open.length();
            int end = soap.indexOf(close, start);
            if (end < 0) return null;

            return soap.substring(start, end).trim();
        } catch (Exception e) {
            return null;
        }
    }
}