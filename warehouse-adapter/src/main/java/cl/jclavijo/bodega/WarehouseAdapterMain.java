package cl.jclavijo.bodega;

import javax.jms.*;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;

public class WarehouseAdapterMain {
    public static void main(String[] args) {
        ConnectionFactory cf = new ActiveMQConnectionFactory("tcp://localhost:61616");
        try (Connection conn = cf.createConnection()) {
            conn.start();
            Session sess = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);

            MessageConsumer consumer = sess.createConsumer(sess.createQueue("jcl_bodega"));

            System.out.println("[INFO] Bodega iniciada. Escuchando cola: jcl_bodega");

            while (true) {
                Message m = consumer.receive(1000);
                if (m == null) continue;
                if (!(m instanceof TextMessage)) continue;
                String payload = ((TextMessage) m).getText();
                System.out.println("\n[LOG 6] Pedido consumido por Bodega:\n" + payload);
            }
        } catch (Throwable e) {
            System.err.println("[FATAL] Bodega:");
            e.printStackTrace();
            System.exit(1);
        }
    }
}