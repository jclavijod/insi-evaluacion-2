package cl.iplacex.tiendaweb.model;

import java.util.List;

public class PedidoCanonico {
    public String idVenta;
    public String origen;
    public String fechaHora;
    public Cliente cliente;
    public List<Item> detalle;

    public static class Cliente {
        public String nombre;
        public String email;
    }

    public static class Item {
        public String codigo;
        public int cantidad;
    }
}