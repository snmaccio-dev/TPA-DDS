package donatrack.donaciones.domain.donacion;

import donatrack.donaciones.domain.catalogo.Categoria;
import donatrack.donaciones.domain.catalogo.Subcategoria;
import donatrack.donaciones.domain.catalogo.Unidades;
import javax.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "Bien")
public class Bien {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bien_id")
    private Long id;

    @Column(name= "descripcion")
    private String descripcion;

    @Column(name = "foto")
    private String foto;

    @ManyToOne
    @JoinColumn(name = "subcategoria_id")
    private Subcategoria subcategoria;

    @Column(name = "cantidad")
    private double cantidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "condicion")
    private CondicionBien condicion;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    protected Bien() {
    }

    public Bien(String descripcion,
                Subcategoria subcategoria,
                double cantidad,
                CondicionBien condicion) {
        if (descripcion == null || descripcion.isBlank()) {
            throw new IllegalArgumentException("El bien debe tener descripcion.");
        }
        if (subcategoria == null) {
            throw new IllegalArgumentException("El bien debe pertenecer a una subcategoria.");
        }
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad del bien debe ser mayor a cero.");
        }
        this.descripcion = descripcion;
        this.subcategoria = subcategoria;
        this.cantidad = cantidad;
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

    public Categoria getCategoria() {
        return subcategoria.getCategoria();
    }

    public double getCantidad() {
        return cantidad;
    }

    public void setCantidad(double cantidad) {
        this.cantidad = cantidad;
    }

    public Unidades getUnidades() {
        return subcategoria.getUnidades();
    }

    public CondicionBien getCondicion() {
        return condicion;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(LocalDate fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }
}