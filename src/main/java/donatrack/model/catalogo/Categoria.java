package donatrack.model.catalogo;

public class Categoria {

    private String nombre;
    private Subcategoria subcategoria;

    public Categoria(String nombre, Subcategoria subcategoria) {
        this.nombre = nombre;
        this.subcategoria = subcategoria;
    }

    public String getNombre() {
        return nombre;
    }

    public Subcategoria getSubcategoria() {
        return subcategoria;
    }

    public void setSubcategoria(Subcategoria subcategoria) {
        this.subcategoria = subcategoria;
    }
}
