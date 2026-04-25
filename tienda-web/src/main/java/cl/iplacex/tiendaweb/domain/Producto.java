package cl.iplacex.tiendaweb.domain;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlRootElement
public interface Producto {
	
	public String getSku();
	public String getNombre();
	public String getImagen();	
	public Long getCosto();
	public Long getPrecioLista();
	public Long getDescuento();
	public Long getPrecioFinal();
	public Categoria getCategoria();

}
