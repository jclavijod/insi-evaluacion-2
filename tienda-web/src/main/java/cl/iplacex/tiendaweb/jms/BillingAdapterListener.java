package cl.iplacex.tiendaweb.jms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
public class BillingAdapterListener {

    private final ObjectMapper objectMapper;
    private final JmsTemplate jmsTemplate;
    private final String queueFacturacion;

    public BillingAdapterListener(ObjectMapper objectMapper,
                                  JmsTemplate jmsTemplate,
                                  @Value("${queue.facturacion}") String queueFacturacion) {
        this.objectMapper = objectMapper;
        this.jmsTemplate = jmsTemplate;
        this.queueFacturacion = queueFacturacion;
    }

    @JmsListener(destination = "${queue.canonical.pedidos}")
    public void procesarPedidoCanonico(String jsonCanonico) {
        try {
            System.out.println(">>> Billing Adapter recibió JSON canónico: " + jsonCanonico);

            JsonNode pedido = objectMapper.readTree(jsonCanonico);
            String soapRequest = construirSoap(pedido);

            System.out.println("<<< SOAP generado para facturación:");
            System.out.println(soapRequest);

            // Enviar el SOAP (simulado como payload) a la cola de facturación
            jmsTemplate.convertAndSend(queueFacturacion, soapRequest);
            System.out.println(">>> Mensaje SOAP enviado a la cola de facturación: " + queueFacturacion);

        } catch (Exception e) {
            System.err.println("Error en Billing Adapter: " + e.getMessage());
            e.printStackTrace();
            // Aquí podrías reintentar o enviar a una DLQ
        }
    }

    private String construirSoap(JsonNode pedido) {
        String idVenta = escapeXml(text(pedido, "idVenta"));
        String origen = escapeXml(text(pedido, "origen"));
        String fechaHora = escapeXml(text(pedido, "fechaHora"));

        JsonNode cliente = pedido.path("cliente");
        String nombreCliente = escapeXml(text(cliente, "nombre"));
        String rutCliente = escapeXml(text(cliente, "rut"));
        String telefonoCliente = escapeXml(text(cliente, "telefono"));
        String emailCliente = escapeXml(text(cliente, "email"));

        JsonNode entrega = pedido.path("entrega");
        String calleYNumero = escapeXml(text(entrega, "calleYNumero"));
        String comuna = escapeXml(text(entrega, "comuna"));

        StringBuilder detalleXml = new StringBuilder();
        JsonNode detalle = pedido.path("detalle");
        if (detalle.isArray()) {
            for (JsonNode item : detalle) {
                detalleXml.append("""
                        <fac:item>
                          <fac:codigo>%s</fac:codigo>
                          <fac:nombreProducto>%s</fac:nombreProducto>
                          <fac:cantidad>%s</fac:cantidad>
                          <fac:precioLista>%s</fac:precioLista>
                          <fac:costo>%s</fac:costo>
                          <fac:categoria>%s</fac:categoria>
                        </fac:item>
                        """.formatted(
                        escapeXml(text(item, "codigo")),
                        escapeXml(text(item, "nombreProducto")),
                        escapeXml(text(item, "cantidad")),
                        escapeXml(text(item, "precioLista")),
                        escapeXml(text(item, "costo")),
                        escapeXml(text(item, "categoria"))
                ));
            }
        }

        return """
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:fac="http://tecnova.cl/facturacion">
                  <soapenv:Header/>
                  <soapenv:Body>
                    <fac:RegistrarPedido>
                      <fac:idVenta>%s</fac:idVenta>
                      <fac:origen>%s</fac:origen>
                      <fac:fechaHora>%s</fac:fechaHora>
                      <fac:cliente>
                        <fac:nombre>%s</fac:nombre>
                        <fac:rut>%s</fac:rut>
                        <fac:telefono>%s</fac:telefono>
                        <fac:email>%s</fac:email>
                      </fac:cliente>
                      <fac:entrega>
                        <fac:calleYNumero>%s</fac:calleYNumero>
                        <fac:comuna>%s</fac:comuna>
                      </fac:entrega>
                      <fac:detalle>
                        %s
                      </fac:detalle>
                    </fac:RegistrarPedido>
                  </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(
                idVenta,
                origen,
                fechaHora,
                nombreCliente,
                rutCliente,
                telefonoCliente,
                emailCliente,
                calleYNumero,
                comuna,
                detalleXml
        );
    }

    private String text(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || node.isNull()) return "";
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) return "";
        return value.asText("");
    }

    private String escapeXml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<​", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}