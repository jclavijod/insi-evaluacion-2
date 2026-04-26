package cl.jclavijo.facturacion;

import javax.jms.*;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;

public class BillingAdapterMain {
    private static final String BROKER_URL = "tcp://localhost:61616";
    private static final String IN_CANONICAL = "jcl_canonical_pedidos";
    private static final String OUT_SOAP_QUEUE = "jcl_facturacion";
    private static final String OUT_BODEGA = "jcl_bodega";

    public static void main(String[] args) {
        ConnectionFactory cf = new ActiveMQConnectionFactory(BROKER_URL);
        try (Connection conn = cf.createConnection()) {
            conn.start();
            Session sess = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);

            MessageProducer soapProducer = sess.createProducer(sess.createQueue(OUT_SOAP_QUEUE));
            MessageProducer bodegaProducer = sess.createProducer(sess.createQueue(OUT_BODEGA));

            System.out.println("[INFO] Billing Adapter iniciado. Escuchando: " + IN_CANONICAL);

            MessageConsumer consumer = sess.createConsumer(sess.createQueue(IN_CANONICAL));
            while (true) {
                Message m = consumer.receive(1000);
                if (m == null) continue;
                if (!(m instanceof TextMessage)) continue;

                String jsonCanonico = ((TextMessage) m).getText();
                System.out.println("\n[LOG Billing] JSON canónico recibido:\n" + jsonCanonico);

                // Construir SOAP mínimo (ejemplo)
                String soap = "<​soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                        + "<soapenv:Body><RegistrarPedido>"
                        + "<json><![CDATA[" + escapeForCData(jsonCanonico) + "]]></json>"
                        + "</RegistrarPedido></soapenv:Body></soapenv:Envelope>";

                System.out.println("[LOG Billing] Enviando SOAP a legacy (cola: " + OUT_SOAP_QUEUE + ")");
                soapProducer.send(sess.createTextMessage(soap));
                System.out.println(">>> SOAP enviado.");

                // Reenviar a bodega
                System.out.println("[LOG Billing] Reenviando JSON a " + OUT_BODEGA);
                bodegaProducer.send(sess.createTextMessage(jsonCanonico));
                System.out.println(">>> Enviado a bodega.");
            }
        } catch (Throwable e) {
            System.err.println("[FATAL] Billing Adapter:");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static String escapeForCData(String s) {
        if (s == null) return "";
        return s.replace("]]>", "]]]]><![CDATA[>");
    }
}