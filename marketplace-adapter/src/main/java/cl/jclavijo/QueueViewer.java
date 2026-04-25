package cl.jclavijo;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Message;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import jakarta.jms.Destination;
import jakarta.jms.MessageConsumer;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;

public class QueueViewer {
    private static final String BROKER_URL = "tcp://localhost:61616";
    private static final String QUEUE_NAME = "jcl_mkp_pedidos";

    public static void main(String[] args) {
        ConnectionFactory cf = new ActiveMQConnectionFactory(BROKER_URL);
        try (Connection connection = cf.createConnection()) {
            connection.start();
            try (Session session = connection.createSession(Session.AUTO_ACKNOWLEDGE)) {
                Destination dest = session.createQueue(QUEUE_NAME);
                MessageConsumer consumer = session.createConsumer(dest);
                System.out.println("[INFO] Esperando mensaje en " + QUEUE_NAME + " (timeout 5s)...");
                Message msg = consumer.receive(5000);
                if (msg == null) {
                    System.out.println("[INFO] No hay mensajes en la cola.");
                } else if (msg instanceof TextMessage) {
                    String body = ((TextMessage) msg).getText();
                    System.out.println("[INFO] Mensaje recibido:");
                    System.out.println(body);
                } else {
                    System.out.println("[WARN] Mensaje recibido de tipo inesperado: " + msg.getClass());
                }
            }
        } catch (Exception e) {
            System.out.println("[ERROR] Error en QueueViewer:");
            e.printStackTrace();
        }
    }
}