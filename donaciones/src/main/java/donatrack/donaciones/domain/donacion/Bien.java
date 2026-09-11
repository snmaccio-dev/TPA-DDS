package donatrack.donaciones.domain.donacion;

import donatrack.donaciones.domain.catalogo.Categoria;
import donatrack.donaciones.domain.catalogo.Subcategoria;

import java.time.LocalDate;

public class Bien {

    private String descripcion;
    private String foto;
    private Subcategoria subcategoria;
    private double cantidad;
    private Unidades unidades;
    private CondicionBien condicion;
    private LocalDate fechaVencimiento;
    private double pesoKg;
    private double volumenM3;

    public Bien(String descripcion,
                Subcategoria subcategoria,
                double cantidad,
                Unidades unidades,
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
        if (unidades == null) {
            throw new IllegalArgumentException("El bien debe tener una unidad de medida.");
        }
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

    public double getPesoKg() {
        return pesoKg;
    }

    public void setPesoKg(double pesoKg) {
        if (pesoKg < 0) {
            throw new IllegalArgumentException("El peso del bien no puede ser negativo.");
        }
        this.pesoKg = pesoKg;
    }

    public double getVolumenM3() {
        return volumenM3;
    }

    public void setVolumenM3(double volumenM3) {
        if (volumenM3 < 0) {
            throw new IllegalArgumentException("El volumen del bien no puede ser negativo.");
        }
        this.volumenM3 = volumenM3;
    }
}
