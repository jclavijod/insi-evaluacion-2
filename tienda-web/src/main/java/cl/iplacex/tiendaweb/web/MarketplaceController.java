package cl.iplacex.tiendaweb.web;

import cl.iplacex.tiendaweb.jms.MarketplaceProducer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/marketplace")
public class MarketplaceController {

    private final MarketplaceProducer producer;

    public MarketplaceController(MarketplaceProducer producer) {
        this.producer = producer;
    }

    @PostMapping
    public ResponseEntity<String> enviarPrueba(@RequestBody String body) {
        producer.send(body);
        return ResponseEntity.accepted().body("Mensaje enviado a la cola marketplace");
    }
}