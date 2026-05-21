package donatrack.model.necesidad;

import donatrack.model.catalogo.Subcategoria;

public class Necesidad {

    private String descripcion;
    private int cantidad;
    private Subcategoria subcategoria;

    public Necesidad(String descripcion, int cantidad, Subcategoria subcategoria) {
        this.descripcion = descripcion;
        this.cantidad = cantidad;
        this.subcategoria = subcategoria;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public Subcategoria getSubcategoria() {
        return subcategoria;
    }
}
