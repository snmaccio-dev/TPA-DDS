package donatrack.model.catalogo;

public class Subcategoria {

    private String nombre;

    private Categoria categoria;

    public Subcategoria(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Categoria getCategoria(){ return categoria; }

    @Override
    public String toString() {
        return nombre;
    }
}
