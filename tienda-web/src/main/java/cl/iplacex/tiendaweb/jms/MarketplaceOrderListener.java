package cl.iplacex.tiendaweb.jms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

@Component
public class MarketplaceOrderListener {

    private final JmsTemplate jmsTemplate;
    private final ObjectMapper objectMapper;
    private final String colaCanonical;
    private final String marketplaceBaseUrl;
    private final RestTemplate restTemplate = new RestTemplate();

    public MarketplaceOrderListener(JmsTemplate jmsTemplate,
                                   ObjectMapper objectMapper,
                                   @Value("${queue.canonical.pedidos}") String colaCanonical,
                                   @Value("${marketplace.base-url:}") String marketplaceBaseUrl) {
        this.jmsTemplate = jmsTemplate;
        this.objectMapper = objectMapper;
        this.colaCanonical = colaCanonical;
        this.marketplaceBaseUrl = marketplaceBaseUrl;
    }

    @JmsListener(destination = "${queue.mkp.pedidos}")
    public void receiveMessage(String jsonMessage) {
        try {
            System.out.println(">>> MarketplaceListener recibió JSON: " + jsonMessage);
            JsonNode root = objectMapper.readTree(jsonMessage);

            // EIP: Content Enricher - shippingCost
            String shippingCost = "0";
            if (marketplaceBaseUrl != null && !marketplaceBaseUrl.isBlank()) {
                String id = firstText(root, "idVenta", "id", "orderId");
                if (id != null) {
                    try {
                        String url = marketplaceBaseUrl + "/orders/" + id + "/shipping-cost";
                        ResponseEntity<String> resp = restTemplate.getForEntity(url, String.class);
                        if (resp.getStatusCode().is2xxSuccessful()) {
                            shippingCost = resp.getBody();
                        }
                    } catch (Exception ex) {
                        System.err.println("Info: No se pudo enriquecer shipping-cost, se usará 0");
                    }
                }
            }

            // Construir JSON Canónico corregido
            ObjectNode pedido = objectMapper.createObjectNode();
            
            String idVenta = firstText(root, "idVenta", "id", "orderId");
            pedido.put("idVenta", idVenta != null ? idVenta : "MP-" + Instant.now().toEpochMilli());
            pedido.put("origen", "MARKETPLACE");
            pedido.put("fechaHora", Instant.now().toString());

            // Cliente
            ObjectNode cliente = objectMapper.createObjectNode();
            JsonNode cNode = root.path("cliente");
            cliente.put("nombre", firstText(root, "clienteNombre", "customerName", "nombre"));
            if (cliente.get("nombre") == null && !cNode.isMissingNode()) {
                cliente.put("nombre", firstText(cNode, "nombre", "name"));
            }
            cliente.put("rut", firstText(root, "rut", "taxId"));
            cliente.put("telefono", firstText(root, "telefono", "phone"));
            cliente.put("email", firstText(root, "email", "mail"));
            pedido.set("cliente", cliente);

            // Entrega
            ObjectNode entrega = objectMapper.createObjectNode();
            entrega.put("calleYNumero", firstText(root, "direccion", "address", "calle"));
            entrega.put("comuna", firstText(root, "comuna", "city"));
            entrega.put("costoEnvio", shippingCost);
            pedido.set("entrega", entrega);

            // Detalle
            ArrayNode detalle = objectMapper.createArrayNode();
            JsonNode itemsNode = firstArrayNode(root, "items", "detalle", "productos");
            if (itemsNode != null && itemsNode.isArray()) {
                for (JsonNode itemNode : itemsNode) {
                    ObjectNode linea = objectMapper.createObjectNode();
                    linea.put("codigo", firstText(itemNode, "sku", "codigo", "id"));
                    linea.put("nombreProducto", firstText(itemNode, "nombre", "productName", "descripcion"));
                    linea.put("cantidad", itemNode.path("cantidad").asInt(1));
                    linea.put("precioLista", itemNode.path("precio").asLong(0));
                    linea.put("costo", itemNode.path("costo").asLong(0));
                    linea.put("categoria", firstText(itemNode, "categoria", "category"));
                    detalle.add(linea);
                }
            }
            pedido.set("detalle", detalle);

            String jsonCanonico = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(pedido);
            System.out.println("<<< Marketplace convertido a JSON canónico:\n" + jsonCanonico);

            jmsTemplate.convertAndSend(colaCanonical, jsonCanonico);
            System.out.println(">>> Enviado a cola canónica: " + colaCanonical);

        } catch (Exception e) {
            System.err.println("Error en MarketplaceOrderListener: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String firstText(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode val = node.get(key);
            if (val != null && !val.isNull()) return val.asText();
        }
        return null;
    }

    private JsonNode firstArrayNode(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode val = node.get(key);
            if (val != null && val.isArray()) return val;
        }
        return null;
    }
}