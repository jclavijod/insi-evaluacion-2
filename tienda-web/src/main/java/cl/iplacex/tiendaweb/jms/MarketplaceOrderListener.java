package cl.iplacex.tiendaweb.jms;

import cl.iplacex.tiendaweb.model.PedidoCanonico;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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

            // Enriquecer si falta shippingCost consultando el servicio externo
            if (marketplaceBaseUrl != null && !marketplaceBaseUrl.isBlank()) {
                JsonNode scNode = firstNode(root, "shippingCost", "costoEnvio", "envio");
                if (scNode == null || scNode.isNull()) {
                    String id = firstText(root, "idVenta", "id", "orderId", "pedidoId", "numeroPedido");
                    if (id != null && !id.isBlank()) {
                        try {
                            String url = marketplaceBaseUrl + "/orders/" + id + "/shipping-cost";
                            ResponseEntity<String> resp = restTemplate.getForEntity(url, String.class);
                            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null && !resp.getBody().isBlank()) {
                                // Intentamos parsear el body simple (por ejemplo "1200") o JSON { "shippingCost": 1200 }
                                try {
                                    JsonNode bodyNode = objectMapper.readTree(resp.getBody());
                                    if (bodyNode.isNumber() || bodyNode.isTextual()) {
                                        ((com.fasterxml.jackson.databind.node.ObjectNode) root).put("shippingCost", bodyNode.asText());
                                    } else if (bodyNode.has("shippingCost")) {
                                        ((com.fasterxml.jackson.databind.node.ObjectNode) root).set("shippingCost", bodyNode.get("shippingCost"));
                                    }
                                } catch (Exception e) {
                                    // si no es JSON, guardamos como texto
                                    ((com.fasterxml.jackson.databind.node.ObjectNode) root).put("shippingCost", resp.getBody());
                                }
                                System.out.println(">>> Enriquecido shippingCost desde marketplace: " + resp.getBody());
                            }
                        } catch (Exception ex) {
                            System.err.println("Warning: no se pudo obtener shipping-cost para id=" + id + " -> " + ex.getMessage());
                        }
                    }
                }
            }

            // Traducción a PedidoCanónico (similar a lo que ya tenías)
            PedidoCanonico pedido = new PedidoCanonico();
            pedido.idVenta = firstText(root, "idVenta", "id", "orderId", "pedidoId", "numeroPedido");
            if (pedido.idVenta == null || pedido.idVenta.isBlank()) {
                pedido.idVenta = "MP-" + Instant.now().toEpochMilli();
            }

            pedido.origen = "MARKETPLACE";
            pedido.fechaHora = firstText(root, "fechaHora", "fecha", "timestamp");
            if (pedido.fechaHora == null || pedido.fechaHora.isBlank()) {
                pedido.fechaHora = Instant.now().toString();
            }

            PedidoCanonico.Cliente cliente = new PedidoCanonico.Cliente();
            cliente.nombre = firstText(root, "clienteNombre", "customerName", "nombreCliente", "nombre");
            cliente.email = firstText(root, "clienteEmail", "email");

            JsonNode clienteNode = root.path("cliente");
            if (!clienteNode.isMissingNode()) {
                if (cliente.nombre == null || cliente.nombre.isBlank()) {
                    cliente.nombre = firstText(clienteNode, "nombre", "name");
                }
                if (cliente.email == null || cliente.email.isBlank()) {
                    cliente.email = firstText(clienteNode, "email");
                }
            }
            pedido.cliente = cliente;

            List<PedidoCanonico.Item> items = new ArrayList<>();
            JsonNode itemsNode = firstArrayNode(root, "detalle", "items", "lineas", "productos");
            if (itemsNode != null && itemsNode.isArray()) {
                for (JsonNode itemNode : itemsNode) {
                    PedidoCanonico.Item item = new PedidoCanonico.Item();
                    item.codigo = firstText(itemNode, "codigo", "sku", "producto", "idProducto");
                    if (item.codigo == null) item.codigo = "";

                    String cantidadStr = firstText(itemNode, "cantidad", "qty", "cantidadProducto");
                    try {
                        item.cantidad = cantidadStr != null ? Integer.parseInt(cantidadStr) : 1;
                    } catch (NumberFormatException e) {
                        item.cantidad = 1;
                    }
                    items.add(item);
                }
            }
            pedido.detalle = items;

            String jsonCanonico = objectMapper.writeValueAsString(pedido);
            System.out.println("<<< Marketplace convertido a JSON canónico: " + jsonCanonico);

            jmsTemplate.convertAndSend(colaCanonical, jsonCanonico);
            System.out.println(">>> Enviado a cola canónica: " + colaCanonical);

        } catch (Exception e) {
            System.err.println("Error en MarketplaceOrderListener: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String firstText(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode value = node.path(key);
            if (!value.isMissingNode() && !value.isNull()) {
                String text = value.asText();
                if (text != null && !text.isBlank()) {
                    return text.trim();
                }
            }
        }
        return null;
    }

    private JsonNode firstArrayNode(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode value = node.path(key);
            if (value != null && value.isArray()) {
                return value;
            }
        }
        return null;
    }

    private JsonNode firstNode(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode value = node.path(key);
            if (!value.isMissingNode() && !value.isNull()) {
                return value;
            }
        }
        return null;
    }
}