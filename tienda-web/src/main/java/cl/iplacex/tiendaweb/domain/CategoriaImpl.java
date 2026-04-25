package cl.iplacex.tiendaweb.domain;

public class CategoriaImpl implements Categoria {
	private int id;
	private String nombre;

	public CategoriaImpl(int id, String nombre) {
		super();
        this.id = id;
		this.nombre = nombre;
	}

    @Override
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() {
		return nombre;
	}
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}
	
	
	

}
