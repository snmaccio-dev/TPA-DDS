package donatrack.donaciones.domain.catalogo;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;

import javax.persistence.Column;
import javax.persistence.OneToMany;
import javax.persistence.Table;


import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Categoria")
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "categoria_id")
    private Long id;

    @Column(name="nombre")
    private String nombre;

    // Una categoría tiene muchas subcategorías
    @OneToMany(mappedBy = "categoria")
    private List<Subcategoria> subcategorias = new ArrayList<>();

    protected Categoria() {
    }

    public Categoria(String nombre) {
        this.nombre = nombre;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public List<Subcategoria> getSubcategorias() {
        return subcategorias;
    }
    
    @Override
    public String toString() {
        return nombre;
    }
}
