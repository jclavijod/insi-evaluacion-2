package cl.iplacex.tiendaweb.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import cl.iplacex.tiendaweb.domain.Producto;
import cl.iplacex.tiendaweb.domain.ProductoImpl;
import org.springframework.stereotype.Service;

@Service
public class ProductoServiceImpl implements ProductoService {

    private static final List<Producto> PRODUCTOS;

    static {
        var catService = new CategoriaServiceImpl();
        var categorias = catService.getCategorias();

        var catNotebooks = catService.getCategoriaById(1);
        var catSmartwatches = catService.getCategoriaById(2);
        var catMemorias = catService.getCategoriaById(3);
        var catImpresoras = catService.getCategoriaById(4);
        var catMousesYTeclados = catService.getCategoriaById(5);

        PRODUCTOS = List.of(
                new ProductoImpl("asus-001", "Notebook Asus Vivobook Go E1404 Intel Core I3 8gb Ram 256gb", 300_000L, 365_000L, catNotebooks, "laptop.webp"),
                new ProductoImpl("imhp-002", "Impresora HP Wifi", 100_000L, 130_000L, catImpresoras, "impresora.webp"),
                new ProductoImpl("moui-003", "Mouse Gamer Inalámbrico NovaStrike", 18_000L, 25_000L, catMousesYTeclados, "mouse.webp"),
                new ProductoImpl("ramf-004", "Memoria RAM Fury DDR4 8GB", 65_000L, 73_990L, catMemorias, "ram.webp"),
                new ProductoImpl("smar-005", "Smartwatch Mujer Olsen", 30_000L, 60_000L, catSmartwatches, "smartwatch.webp"),
                new ProductoImpl("keyb-006", "Teclado Gamer Kumara", 25_000L, 34_000L, catMousesYTeclados, "teclado.webp")
        );
    }

    @Override
    public List<? extends Producto> getProductos() {
        return PRODUCTOS;
    }

    @Override
    public List<? extends Producto> getProductosByCategoriaId(int catId) {
        return PRODUCTOS.stream().filter(p -> p.getCategoria().getId() == catId).toList();
    }

    @Override
    public Optional<Producto> getProductoBySku(String sku) {
        return PRODUCTOS.stream().filter(p -> Objects.equals(sku, p.getSku())).findFirst();
    }
}
