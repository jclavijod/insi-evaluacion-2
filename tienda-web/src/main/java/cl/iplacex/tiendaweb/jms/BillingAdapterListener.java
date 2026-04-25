package cl.iplacex.tiendaweb.jms;

import cl.iplacex.tiendaweb.model.PedidoCanonico;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
public class BillingAdapterListener {
    private final ObjectMapper mapper;
    private final JmsTemplate jms;
    @Value("${queue.facturacion}") private String colaOut;

    public BillingAdapterListener(ObjectMapper mapper, JmsTemplate jms) {
        this.mapper = mapper;
        this.jms = jms;
    }

    @JmsListener(destination = "${queue.canonical.pedidos}")
    public void onMessage(String json) {
        try {
            System.out.println(">>> Recibido Canónico: " + json);
            PedidoCanonico p = mapper.readValue(json, PedidoCanonico.class);
            String soap = """
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                  <soap:Body>
                    <id>%s</id><origen>%s</origen>
                  </soap:Body>
                </soap:Envelope>""".formatted(p.idVenta, p.origen);
            
            System.out.println("<<< Enviando SOAP a " + colaOut);
            jms.convertAndSend(colaOut, soap);
        } catch (Exception e) { e.printStackTrace(); }
    }
}