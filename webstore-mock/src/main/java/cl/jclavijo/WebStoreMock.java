package cl.jclavijo;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;

public class WebStoreMock {
    public static void main(String[] args) throws Exception {
        ConnectionFactory cf = new ActiveMQConnectionFactory("tcp://localhost:61616");

        try (Connection conn = cf.createConnection();
             Session sess = conn.createSession(false, Session.AUTO_ACKNOWLEDGE)) {

            conn.start();

            MessageProducer prod = sess.createProducer(sess.createQueue("jcl_web_pedidos"));

            String xml = """
                    <pedido>
                      <id>WEB-999</id>
                      <cliente>José Clavijo</cliente>
                      <clienteEmail>jose.clavijo@correo.cl</clienteEmail>
                      <total>150000</total>
                      <items>
                        <item>
                          <codigo>P-001</codigo>
                          <cantidad>2</cantidad>
                        </item>
                      </items>
                    </pedido>
                    """;

            System.out.println("[INFO] Enviando pedido XML de prueba a jcl_web_pedidos ...");
            prod.send(sess.createTextMessage(xml));
            System.out.println("[OK] Enviado.");
            // esperar un poco para que broker procese antes de cerrar (evita threads en librería)
            Thread.sleep(300);
        }
    }
}