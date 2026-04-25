package cl.iplacex.tiendaweb.jms;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
public class LegacyBillingConsumer {

    private final JmsTemplate jmsTemplate;
    private final String queueRespuesta;

    public LegacyBillingConsumer(JmsTemplate jmsTemplate,
                                 @Value("${queue.facturacion.respuesta:jcl_facturacion_respuesta}") String queueRespuesta) {
        this.jmsTemplate = jmsTemplate;
        this.queueRespuesta = queueRespuesta;
    }

    @JmsListener(destination = "${queue.facturacion:jcl_facturacion}")
    public void consumirSoap(String soapPayload) {
        try {
            System.out.println(">>> LEGADO (simulado) recibió SOAP en jcl_facturacion:");
            System.out.println(soapPayload);

            // Simular procesamiento
            String correlacion = "ACK-" + System.currentTimeMillis();
            String respuesta = "<respuesta><status>OK</status><correlacion>" + correlacion + "</correlacion></respuesta>";

            // Enviar confirmación a una cola de respuesta (opcional)
            jmsTemplate.convertAndSend(queueRespuesta, respuesta);
            System.out.println(">>> LEGADO (simulado) envió confirmación a: " + queueRespuesta + " -> " + respuesta);

        } catch (Exception e) {
            System.err.println("Error en LegacyBillingConsumer: " + e.getMessage());
            e.printStackTrace();
        }
    }
}