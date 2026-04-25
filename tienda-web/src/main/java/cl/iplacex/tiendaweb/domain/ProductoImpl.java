package cl.iplacex.tiendaweb.domain;

import cl.iplacex.tiendaweb.ext.carrito.adapter.CategoriaAdapter;
import cl.iplacex.tiendaweb.ext.carrito.adapter.ProductoAdapter;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class ProductoImpl implements Producto {

	private String sku;
	private String nombre;
	private Long costo;
	private Long precioLista;
    @XmlJavaTypeAdapter(CategoriaAdapter.class)
	private Categoria categoria;
	private String imagen;

    public ProductoImpl() {
    }

    public ProductoImpl(String sku, String titulo, Long costo, Long precioLista, Categoria categoria, String imagen) {
		super();
		this.sku = sku;
		this.nombre = titulo;
		this.costo = costo;
		this.precioLista = precioLista;
		this.categoria = categoria;
		this.imagen = imagen;
	}

	public String getSku() {
		return sku;
	}

	public void setSku(String sku) {
		this.sku = sku;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public Long getCosto() {
		return costo;
	}

	public void setCosto(Long costo) {
		this.costo = costo;
	}

	public Long getPrecioLista() {
		return precioLista;
	}

	public void setPrecioLista(Long precioLista) {
		this.precioLista = precioLista;
	}

	public Categoria getCategoria() {
		return categoria;
	}

	public void setCategoria(Categoria categoria) {
		this.categoria = categoria;
	}
	
	public String getImagen() {
		return imagen;
	}

	public void setImagen(String imagen) {
		this.imagen = imagen;
	}

	@Override
	public Long getDescuento() {
		return 0L;
	}

	@Override
	public Long getPrecioFinal() {
		return getPrecioLista() - getDescuento();
	}

}
