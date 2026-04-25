package cl.jclavijo;

import jakarta.jms.*;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;

public class WebStoreMock {
    public static void main(String[] args) throws Exception {
        ConnectionFactory cf = new ActiveMQConnectionFactory("tcp://localhost:61616");
        try (Connection conn = cf.createConnection(); 
             Session sess = conn.createSession(Session.AUTO_ACKNOWLEDGE)) {
            
            MessageProducer prod = sess.createProducer(sess.createQueue("jcl_web_pedidos"));
            
            String xml = "<pedido><id>WEB-999</id><cliente>Jose Clavijo</cliente><total>150000</total></pedido>";
            
            System.out.println("[INFO] Enviando pedido XML de prueba...");
            prod.send(sess.createTextMessage(xml));
            System.out.println("[OK] Enviado.");
        }
    }
}