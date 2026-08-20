package donatrack.model.donacion;

import donatrack.model.catalogo.Categoria;
import donatrack.model.catalogo.Subcategoria;

import java.time.LocalDate;

public class Bien {

    private String descripcion;
    private String foto;
    private Subcategoria subcategoria;
    private double cantidad;
    private Unidades unidades;
    private CondicionBien condicion;
    private LocalDate fechaVencimiento;

    public Bien(String descripcion,
                Subcategoria subcategoria,
                double cantidad,
                Unidades unidades,
                CondicionBien condicion) {
        this.descripcion = descripcion;
        this.subcategoria = subcategoria;
        this.cantidad = cantidad;
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
        return unidades;
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
