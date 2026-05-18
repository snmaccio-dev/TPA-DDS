package donatrack.model.donacion;

import donatrack.model.catalogo.Subcategoria;

public class Bien {

    private String descripcion;
    private String foto;
    private Subcategoria subcategoria;
    private Unidades unidades;

    public Bien(String descripcion, Subcategoria subcategoria, Unidades unidades) {
        this.descripcion = descripcion;
        this.subcategoria = subcategoria;
        this.unidades = unidades;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getFoto() {
        return foto;
    }

    public void setFoto(String foto) {
        this.foto = foto;
    }

    public Subcategoria getSubcategoria() {
        return subcategoria;
    }

    public Unidades getUnidades() {
        return unidades;
    }
}
