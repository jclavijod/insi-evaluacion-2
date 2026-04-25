package cl.jclavijo;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;

public class WebStoreMock {

    private static final String BROKER_URL = "tcp://localhost:61616";
    private static final String OUT_WEB = "jcl_web_pedidos";

    public static void main(String[] args) throws Exception {
        ConnectionFactory cf = new ActiveMQConnectionFactory(BROKER_URL);

        try (Connection conn = cf.createConnection();
             Session sess = conn.createSession(false, Session.AUTO_ACKNOWLEDGE)) {

            MessageProducer prod = sess.createProducer(sess.createQueue(OUT_WEB));

            String xml = """
                    <pedido>
                        <id>WEB-999</id>
                        <cliente>Jose Clavijo</cliente>
                        <total>150000</total>
                    </pedido>
                    """.trim();

            System.out.println("[INFO] Enviando pedido XML de prueba...");
            prod.send(sess.createTextMessage(xml));
            System.out.println("[OK] Enviado a cola " + OUT_WEB);
        }
    }
}