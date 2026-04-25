package cl.iplacex.tiendaweb.jms;

import cl.iplacex.tiendaweb.model.PedidoCanonico;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class MarketplaceOrderListener {

    private final JmsTemplate jmsTemplate;
    private final ObjectMapper objectMapper;
    private final String colaCanonical;

    public MarketplaceOrderListener(JmsTemplate jmsTemplate,
                                    ObjectMapper objectMapper,
                                    @Value("${queue.canonical.pedidos}") String colaCanonical) {
        this.jmsTemplate = jmsTemplate;
        this.objectMapper = objectMapper;
        this.colaCanonical = colaCanonical;
    }

    @JmsListener(destination = "${queue.marketplace.pedidos}")
    public void receiveMessage(String jsonMessage) {
        try {
            System.out.println(">>> Marketplace recibió JSON: " + jsonMessage);

            JsonNode root = objectMapper.readTree(jsonMessage);

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
                    if (item.codigo == null) {
                        item.codigo = "";
                    }

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
            System.err.println("Error traduciendo Marketplace -> Canonical: " + e.getMessage());
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
}