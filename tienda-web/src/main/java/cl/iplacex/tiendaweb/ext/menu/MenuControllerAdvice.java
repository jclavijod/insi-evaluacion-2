package cl.iplacex.tiendaweb.ext.menu;

import java.util.ArrayList;
import java.util.List;

import cl.iplacex.tiendaweb.ext.menu.util.Slugger;
import cl.iplacex.tiendaweb.service.CategoriaService;
import cl.iplacex.tiendaweb.service.CategoriaServiceImpl;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class MenuControllerAdvice {

    private static final List<ItemMenu> MENU;

    static {
        CategoriaService categoriaService = new CategoriaServiceImpl();

        List<ItemMenu> menu = new ArrayList<>();
        menu.add(new ItemMenuImpl("Inicio", "/"));

        var categorias = categoriaService.getCategorias().stream()
                .map(c -> new ItemMenuImpl(
                        c.getNombre(),
                        "/categoria/" + c.getId() + "/" + Slugger.toSlug(c.getNombre())
                ))
                .toList();

        menu.addAll(categorias);
        MENU = List.copyOf(menu); // inmutable
    }

    @ModelAttribute("menu")
    public List<ItemMenu> menu() {
        return MENU;
    }
}
