package donatrack.donaciones.domain.catalogo;

import javax.persistence.*;

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

    // Cada subcategoria pertenece a una categoria
    @ManyToOne
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

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
