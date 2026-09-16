package donatrack.donaciones.domain.catalogo;

import donatrack.donaciones.domain.donacion.Bien;
import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.necesidad.Necesidad;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Subcategoria") // Agrego para coincidir con el DER
public class Subcategoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="nombre")
    private String nombre;

    // Agrego @Enumerated asumiendo que Unidades es un Enum (ej: KILOS, LITROS)
    @Enumerated(EnumType.STRING)
    @Column(name="unidades")
    private Unidades unidades;

    // Relaciones muchos a muchos
    @ManyToOne
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @OneToMany(mappedBy = "subcategoria")
    private List<Necesidad> necesidades = new ArrayList<>();

    @OneToMany(mappedBy = "subcategoria")
    private List<Donacion> donaciones = new ArrayList<>();

    @OneToMany(mappedBy = "subcategoria")
    private List<Bien> bienes = new ArrayList<>();

    protected Subcategoria() {
    }

    public Subcategoria(String nombre, Categoria categoria, Unidades unidades) {
        if (unidades == null) {
            throw new IllegalArgumentException("La subcategoria debe tener una unidad de medida.");
        }
        this.nombre = nombre;
        this.categoria = categoria;
        this.unidades = unidades;
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