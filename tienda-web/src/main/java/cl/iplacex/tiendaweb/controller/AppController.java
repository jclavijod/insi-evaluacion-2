package cl.iplacex.tiendaweb.controller;

import cl.iplacex.tiendaweb.service.CategoriaService;
import cl.iplacex.tiendaweb.service.ProductoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class AppController {

    private CategoriaService categoriaService;
    private ProductoService productoService;

    public AppController(CategoriaService categoriaService, ProductoService productoService) {
        this.categoriaService = categoriaService;
        this.productoService = productoService;
    }

    @GetMapping("/")
    String inicio(Model model) {
        var productos = productoService.getProductos();
        model.addAttribute("productos", productos);
        return "inicio";
    }

    @GetMapping("/categoria/{catId}/{slug}")
    String article(@PathVariable int catId, @PathVariable String slug, Model model) {
        var categoria = categoriaService.getCategoriaById(catId);
        var productos = productoService.getProductosByCategoriaId(catId);
        model.addAttribute("categoria", categoria);
        model.addAttribute("productos", productos);
        return "categoria";
    }

}
