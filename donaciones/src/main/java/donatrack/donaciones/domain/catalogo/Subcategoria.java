package donatrack.donaciones.domain.catalogo;

import donatrack.donaciones.domain.necesidad.Necesidad;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Subcategoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="nombre")
    private String nombre;

    @ManyToOne
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @Column(name="unidades")
    private Unidades unidades;

    @OneToMany(mappedBy = "subcategoria")
    private List<Necesidad> necesidades = new ArrayList<>();

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