package cl.jclavijo.facturacion;

import javax.jms.*;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;

public class FacturacionAdapterMain {
    public static void main(String[] args) {
        ConnectionFactory cf = new ActiveMQConnectionFactory("tcp://localhost:61616");
        try (Connection conn = cf.createConnection()) {
            conn.start();
            Session sess = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer prod = sess.createProducer(sess.createQueue("jcl_facturacion_respuesta"));

            System.out.println("[INFO] Simulador Sistema de Facturacion Legacy esperando SOAP en jcl_facturacion...");

            MessageConsumer consumer = sess.createConsumer(sess.createQueue("jcl_facturacion"));
            while (true) {
                Message m = consumer.receive(1000);
                if (m == null) continue;
                if (!(m instanceof TextMessage)) continue;
                System.out.println("[LOG 5] SOAP recibido (simulador). Generando ACK...");
                prod.send(sess.createTextMessage("{\"status\":\"OK\", \"mensaje\":\"Factura generada\"}"));
                System.out.println(">>> ACK enviado a jcl_facturacion_respuesta");
            }
        } catch (Throwable e) {
            System.err.println("[FATAL] Simulador Facturacion:");
            e.printStackTrace();
            System.exit(1);
        }
    }
}