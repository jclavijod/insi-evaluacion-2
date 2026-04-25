package cl.jclavijo;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.jms.*;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class MarketplaceAdapter {

    // Configuración de la cola y el broker
    private static final String BROKER_URL = "tcp://localhost:61616";
    private static final String QUEUE_NAME = "jcl_mkp_pedidos";

    public static void main(String[] args) {
        try {
            // 1. Consumir los pedidos del Marketplace (REST)
            String ordersJson = getRequest("http://localhost:8091/orders/today");
            JsonArray orders = JsonParser.parseString(ordersJson).getAsJsonArray();

            // 2. Preparar conexión a Artemis
            ConnectionFactory cf = new ActiveMQConnectionFactory(BROKER_URL);
            try (Connection connection = cf.createConnection();
                 Session session = connection.createSession(Session.AUTO_ACKNOWLEDGE)) {

                Destination destination = session.createQueue(QUEUE_NAME);
                MessageProducer producer = session.createProducer(destination);
                Gson gson = new Gson();

                System.out.println("Procesando " + orders.size() + " pedidos...");

                for (int i = 0; i < orders.size(); i++) {
                    JsonObject order = orders.get(i).getAsJsonObject();
                    String id = order.get("id").getAsString();

                    // 3. Patrón CONTENT ENRICHER: Consultar costo de envío por cada ID
                    System.out.println("Enriqueciendo pedido ID: " + id);
                    String shippingJson = getRequest("http://localhost:8091/orders/" + id + "/shipping-cost");
                    JsonObject shippingInfo = JsonParser.parseString(shippingJson).getAsJsonObject();

                    // Agregamos el costo de envío al objeto original
                    order.addProperty("costoEnvio", shippingInfo.get("costoEnvio").getAsInt());
                    order.addProperty("monedaEnvio", shippingInfo.get("moneda").getAsString());

                    // 4. Enviar a la cola Artemis
                    TextMessage message = session.createTextMessage(gson.toJson(order));
                    producer.send(message);
                    System.out.println("Enviado a " + QUEUE_NAME + ": " + id);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Método auxiliar para hacer peticiones GET a los servicios REST
    private static String getRequest(String url) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
}