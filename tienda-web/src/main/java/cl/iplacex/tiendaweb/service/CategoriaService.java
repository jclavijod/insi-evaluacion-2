package cl.iplacex.tiendaweb.service;

import cl.iplacex.tiendaweb.domain.Categoria;

import java.util.List;


public interface CategoriaService {

	public List<? extends Categoria> getCategorias();
    public Categoria getCategoriaById(int id);
	
}
