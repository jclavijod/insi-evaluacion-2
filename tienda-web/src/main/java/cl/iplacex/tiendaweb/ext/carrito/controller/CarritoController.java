package cl.iplacex.tiendaweb.ext.carrito.controller;

import cl.iplacex.tiendaweb.ext.carrito.domain.Carrito;
import cl.iplacex.tiendaweb.ext.carrito.domain.CarritoImpl;
import cl.iplacex.tiendaweb.ext.carrito.domain.LineaPedidoImpl;
import cl.iplacex.tiendaweb.ext.carrito.domain.Pedido;
import cl.iplacex.tiendaweb.ext.carrito.event.PedidoCompletadoEvent;
import cl.iplacex.tiendaweb.service.CategoriaService;
import cl.iplacex.tiendaweb.service.ProductoService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.stream.Collectors;

@Controller
public class CarritoController {
    private final Logger logger = LoggerFactory.getLogger(CarritoController.class);
    private final ProductoService productoService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public CarritoController(ProductoService productoService, ApplicationEventPublisher applicationEventPublisher) {
        this.productoService = productoService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @PostMapping("/comprar")
    String comprar(HttpSession session, Model model, @ModelAttribute Pedido pedido) {
        Carrito carrito = (Carrito) session.getAttribute("carrito");
        pedido.setItems(
                carrito.getLineasPedido().stream()
                        .map(lp -> (LineaPedidoImpl) lp)   // casteo explícito
                        .collect(Collectors.toList())
        );
        model.addAttribute("pedido", pedido);
        logger.info("Registrando venta de {} pesos, será enviada a {}", pedido.getTotal(), pedido.getDireccionDespacho().getComuna());
        applicationEventPublisher.publishEvent(new PedidoCompletadoEvent(pedido));
        return "compra";
    }

    @GetMapping("/carrito")
    String carrito(Model model) {
        var pedido = new Pedido();
        model.addAttribute("pedido", pedido);
        return "carrito";
    }

    @GetMapping("/carrito/remove")
    String remove(
            HttpSession session,
            @RequestParam String sku,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Carrito carrito = (Carrito) session.getAttribute("carrito");
        if(carrito == null) carrito = new CarritoImpl();
        carrito.quitarProducto(sku);
        return "redirect:/carrito";
    }

    @GetMapping("/carrito/add")
    String add(
            HttpSession session,
            @RequestParam String sku,
            @RequestParam(name = "cant", defaultValue = "1") int cantidad,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Carrito carrito = (Carrito) session.getAttribute("carrito");
        if(carrito == null) carrito = new CarritoImpl();
        var productoOpt = productoService.getProductoBySku(sku);
        if( productoOpt.isPresent() ) {
            carrito.agregarProducto(productoOpt.get(), cantidad);
            session.setAttribute("carrito", carrito);
            redirectAttributes.addFlashAttribute("mensaje", "Se agregó al carro: "+productoOpt.get().getNombre());
        }
        return "redirect:/carrito";
    }
}
