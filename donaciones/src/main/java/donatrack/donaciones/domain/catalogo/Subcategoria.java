package donatrack.donaciones.domain.catalogo;

public class Subcategoria {

    private String nombre;
    private Categoria categoria;
    private Unidades unidades;

    public Subcategoria(String nombre, Categoria categoria, Unidades unidades) {
        if (unidades == null) {
            throw new IllegalArgumentException("La subcategoria debe tener una unidad de medida.");
        }
        this.nombre = nombre;
        this.categoria = categoria;
        this.unidades = unidades;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public Unidades getUnidades() {
        return unidades;
    }

    @Override
    public String toString() {
        return nombre;
    }
}