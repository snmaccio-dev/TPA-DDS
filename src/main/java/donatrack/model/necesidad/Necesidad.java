package donatrack.model.necesidad;

import donatrack.model.catalogo.Subcategoria;

public abstract class Necesidad {

  private static long proximoId = 1;

  private final long id;

  protected String descripcion;
  protected int cantidad;
  protected Subcategoria subcategoria;

  public Necesidad(
      String descripcion,
      int cantidad,
      Subcategoria subcategoria
  ) {
    this.id = proximoId++;
    this.descripcion = descripcion;
    this.cantidad = cantidad;
    this.subcategoria = subcategoria;
  }

  public long getId() {
    return id;
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


  // Metodos para el CRUD
  public void setDescripcion(String descripcion) {
    this.descripcion = descripcion;
  }

  public void setCantidad(int cantidad) {
    this.cantidad = cantidad;
  }

  public void setSubcategoria(Subcategoria subcategoria) {
    this.subcategoria = subcategoria;
  }
}