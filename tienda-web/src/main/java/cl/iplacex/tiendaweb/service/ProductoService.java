package cl.iplacex.tiendaweb.service;

import cl.iplacex.tiendaweb.domain.Producto;

import java.util.List;
import java.util.Optional;

public interface ProductoService {

	public List<? extends Producto> getProductos();
    public List<? extends Producto> getProductosByCategoriaId(int catId);
	public Optional<Producto> getProductoBySku(String sku);
}
