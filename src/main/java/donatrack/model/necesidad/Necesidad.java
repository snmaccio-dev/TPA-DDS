package donatrack.model.necesidad;

import donatrack.model.catalogo.Subcategoria;

public abstract class Necesidad {

  protected String descripcion;
  protected int cantidad;
  protected Subcategoria subcategoria;

  public Necesidad(String descripcion, int cantidad, Subcategoria subcategoria) {
    this.descripcion = descripcion;
    this.cantidad = cantidad;
    this.subcategoria = subcategoria;
  }

  public int getCantidad() {
    return cantidad;
  }

  public Subcategoria getSubcategoria() {
    return subcategoria;
  }

  public String getDescripcion() {
    return descripcion;
  }

  public abstract boolean esExtraordinaria();
}