package donatrack.model.logistica;

import donatrack.model.donacion.Donacion;

import java.util.List;

public class DestinoEntrega {

  private String direccion;
  private List<Donacion> donaciones;

  public DestinoEntrega(String direccion, List<Donacion> donaciones) {
    this.direccion = direccion;
    this.donaciones = donaciones;
  }

  public String getDireccion() {
    return direccion;
  }

  public List<Donacion> getDonaciones() {
    return donaciones;
  }
}