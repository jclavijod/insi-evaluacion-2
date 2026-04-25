package cl.iplacex.tiendaweb.jms;

import cl.iplacex.tiendaweb.model.PedidoCanonico;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class BillingAdapterListener {

    private final ObjectMapper objectMapper;

    public BillingAdapterListener(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @JmsListener(destination = "${queue.canonical.pedidos}")
    public void procesarPedidoCanonico(String jsonCanonico) {
        try {
            System.out.println(">>> Billing Adapter recibió JSON canónico: " + jsonCanonico);

            PedidoCanonico pedido = objectMapper.readValue(jsonCanonico, PedidoCanonico.class);

            String soapRequest = construirSoap(pedido);

            System.out.println("<<< SOAP generado para facturación:");
            System.out.println(soapRequest);

            // Aquí después podrías enviar realmente al servicio SOAP
            System.out.println(">>> Envío SOAP simulado a: " + "jcl_facturacion");

        } catch (Exception e) {
            System.err.println("Error en Billing Adapter: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String construirSoap(PedidoCanonico pedido) {
        String idVenta = escapeXml(pedido.idVenta);
        String origen = escapeXml(pedido.origen);
        String fechaHora = escapeXml(pedido.fechaHora);
        String nombreCliente = pedido.cliente != null ? escapeXml(pedido.cliente.nombre) : "";
        String emailCliente = pedido.cliente != null ? escapeXml(pedido.cliente.email) : "";

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
                        <fac:email>%s</fac:email>
                      </fac:cliente>
                    </fac:RegistrarPedido>
                  </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(idVenta, origen, fechaHora, nombreCliente, emailCliente);
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