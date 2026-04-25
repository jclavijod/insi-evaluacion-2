package cl.jclavijo.bodega;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageListener;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;

public class WarehouseAdapterMain {

    private static final String BROKER_URL = "tcp://localhost:61616";
    private static final String IN_BODEGA = "jcl_bodega";

    public static void main(String[] args) {
        ConnectionFactory cf = new ActiveMQConnectionFactory(BROKER_URL);

        try {
            Connection connection = cf.createConnection();
            connection.start();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            MessageConsumer consumer = session.createConsumer(session.createQueue(IN_BODEGA));

            System.out.println("[INFO] Bodega iniciada.");
            System.out.println("[INFO] Escuchando cola: " + IN_BODEGA);

            consumer.setMessageListener((MessageListener) message -> {
                try {
                    if (message instanceof TextMessage textMessage) {
                        String payload = textMessage.getText();
                        System.out.println("\n----");
                        System.out.println("[LOG 6] Pedido consumido por Bodega:");
                        System.out.println(payload);
                    }
                } catch (Exception e) {
                    System.err.println("Error en Bodega:");
                    e.printStackTrace();
                }
            });

            Thread.sleep(Long.MAX_VALUE);

        } catch (Exception e) {
            System.err.println("[ERROR] Bodega no pudo iniciar:");
            e.printStackTrace();
        }
    }
}