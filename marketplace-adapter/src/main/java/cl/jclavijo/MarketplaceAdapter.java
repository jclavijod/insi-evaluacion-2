package cl.jclavijo;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class MarketplaceAdapter {

    private static final String BROKER_URL = "tcp://localhost:61616";
    private static final String QUEUE_NAME = "jcl_mkp_pedidos";

    public static void main(String[] args) {
        try {
            // 1. Consumir los pedidos del Marketplace
            String ordersJson = getRequest("http://localhost:8091/orders/today");
            JsonArray orders = JsonParser.parseString(ordersJson).getAsJsonArray();

            System.out.println("[INFO] Pedidos recibidos del Marketplace: " + orders.size());

            // 2. Conexión a Artemis
            ConnectionFactory cf = new ActiveMQConnectionFactory(BROKER_URL);

            try (Connection connection = cf.createConnection();
                 Session session = connection.createSession(Session.AUTO_ACKNOWLEDGE)) {

                Destination destination = session.createQueue(QUEUE_NAME);
                MessageProducer producer = session.createProducer(destination);
                Gson gson = new Gson();

                // 3. Procesar cada pedido
                for (int i = 0; i < orders.size(); i++) {
                    JsonObject order = orders.get(i).getAsJsonObject();
                    String id = order.get("id").getAsString();

                    System.out.println("[INFO] Procesando Pedido ID: " + id);

                    // Content Enricher: obtener costo de envío
                    System.out.println("[INFO] Aplicando Content Enricher: Consultando costo de envío para ID: " + id);

                    try {
                        String shippingJson = getRequest("http://localhost:8091/orders/" + id + "/shipping-cost");
                        JsonObject shippingInfo = JsonParser.parseString(shippingJson).getAsJsonObject();

                        int costo = shippingInfo.has("costoEnvio") ? shippingInfo.get("costoEnvio").getAsInt() : 0;
                        String moneda = shippingInfo.has("moneda") ? shippingInfo.get("moneda").getAsString() : "CLP";

                        order.addProperty("costoEnvio", costo);
                        order.addProperty("monedaEnvio", moneda);

                        System.out.println("[INFO] Costo obtenido: " + costo + " " + moneda);
                    } catch (Exception ex) {
                        System.out.println("[WARN] No se pudo obtener el costo de envío para ID: " + id + " -> " + ex.getMessage());
                        order.addProperty("costoEnvio", 0);
                        order.addProperty("monedaEnvio", "CLP");
                    }

                    // 4. Enviar a la cola Artemis
                    TextMessage message = session.createTextMessage(gson.toJson(order));
                    producer.send(message);

                    System.out.println("[INFO] Enviado a " + QUEUE_NAME + ": " + id);
                }
            }

        } catch (Exception e) {
            System.out.println("[ERROR] Error en MarketplaceAdapter:");
            e.printStackTrace();
        }
    }

    private static String getRequest(String url) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return response.body();
        } else {
            throw new RuntimeException(
                    "GET " + url + " returned status " + response.statusCode() + " - body: " + response.body()
            );
        }
    }
}