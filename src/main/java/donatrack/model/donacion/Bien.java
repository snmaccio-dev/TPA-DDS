package donatrack.model.donacion;

import donatrack.model.catalogo.Categoria;
import donatrack.model.catalogo.Subcategoria;
import donatrack.model.donacion.CondicionBien;

import java.time.LocalDate;

public class Bien {

    private String descripcion;
    private String nombre;
    private String foto;
    private Subcategoria subcategoria;
    private Unidades unidades;
    private CondicionBien condicion;
    private LocalDate fechaVencimiento;

    public Bien(String descripcion,
                Subcategoria subcategoria,
                Unidades unidades,
                CondicionBien condicion) {
        this.descripcion = descripcion;
        this.subcategoria = subcategoria;
        this.unidades = unidades;
        this.condicion = condicion;
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

    public Categoria getCategoria(){
      return subcategoria.getCategoria();
    }

    public Unidades getUnidades() {
        return unidades;
    }

    public String getNombre(){
      return nombre;
    }
}
