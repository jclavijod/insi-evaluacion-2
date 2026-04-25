package cl.jclavijo.facturacion;

import javax.jms.*;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;

public class FacturacionAdapterMain {
    public static void main(String[] args) throws Exception {
        ConnectionFactory cf = new ActiveMQConnectionFactory("tcp://localhost:61616");
        try (Connection conn = cf.createConnection()) {
            conn.start();
            Session sess = conn.createSession(Session.AUTO_ACKNOWLEDGE);
            MessageProducer prod = sess.createProducer(sess.createQueue("jcl_facturacion_respuesta"));
            
            System.out.println("[INFO] Simulador Facturacion Legacy esperando SOAP...");
            sess.createConsumer(sess.createQueue("jcl_facturacion")).setMessageListener(m -> {
                try {
                    System.out.println("[LOG 5] SOAP Recibido. Generando ACK...");
                    prod.send(sess.createTextMessage("{\"status\":\"OK\", \"mensaje\":\"Factura generada\"}"));
                } catch (Exception e) { e.printStackTrace(); }
            });
            Thread.sleep(Long.MAX_VALUE);
        }
    }
}
