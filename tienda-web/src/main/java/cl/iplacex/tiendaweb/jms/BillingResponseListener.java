package cl.iplacex.tiendaweb.jms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class BillingResponseListener {

    private final ObjectMapper objectMapper;
    private final JmsTemplate jmsTemplate;
    private final String colaBodega;

    public BillingResponseListener(
            ObjectMapper objectMapper,
            JmsTemplate jmsTemplate,
            @Value("${queue.bodega}") String colaBodega
    ) {
        this.objectMapper = objectMapper;
        this.jmsTemplate = jmsTemplate;
        this.colaBodega = colaBodega;
    }

    @JmsListener(destination = "${queue.facturacion.respuesta}")
    public void recibirRespuestaFacturacion(String ackJson) {
        try {
            System.out.println("[LOG 5] Respuesta recibida del sistema SOAP / ACK:");
            System.out.println(ackJson);

            JsonNode root;
            try {
                root = objectMapper.readTree(ackJson);
            } catch (Exception parseError) {
                root = null;
            }

            ObjectNode bodegaMsg = objectMapper.createObjectNode();
            bodegaMsg.put("origen", "FACTURACION");
            bodegaMsg.put("fechaHora", Instant.now().toString());

            if (root != null) {
                bodegaMsg.put("idVenta", root.path("idVenta").asText("N/A"));
                bodegaMsg.put("estado", root.path("status").asText("OK"));
                bodegaMsg.put("mensaje", root.path("mensaje").asText("ACK recibido"));
            } else {
                bodegaMsg.put("idVenta", "N/A");
                bodegaMsg.put("estado", "OK");
                bodegaMsg.put("mensaje", ackJson);
            }

            String payloadBodega = objectMapper.writeValueAsString(bodegaMsg);
            jmsTemplate.convertAndSend(colaBodega, payloadBodega);

            System.out.println(">>> Pedido reenviado a bodega: " + colaBodega);
            System.out.println(payloadBodega);

        } catch (Exception e) {
            System.err.println("Error procesando respuesta de facturación: " + e.getMessage());
            e.printStackTrace();
        }
    }
}