package cl.iplacex.tiendaweb.ext.carrito.controller;

import cl.iplacex.tiendaweb.ext.carrito.domain.Carrito;
import cl.iplacex.tiendaweb.ext.carrito.domain.CarritoImpl;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class CarritoControllerAdvice {

    @ModelAttribute("carrito")
    public Carrito carrito(HttpSession session) {
        Carrito carrito = (Carrito) session.getAttribute("carrito");
        if( carrito == null ) carrito = new CarritoImpl();
        return carrito;
    }

}
