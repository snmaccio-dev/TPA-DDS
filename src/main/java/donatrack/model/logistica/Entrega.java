package donatrack.model.logistica;

import donatrack.model.donacion.Donacion;
import donatrack.model.entidad.EntidadBeneficiaria;

public class Entrega {

  private final long id;
  private final Donacion donacion;
  private final EntidadBeneficiaria destinatario;
  private Camion camion;

  public Entrega(
      long id,
      Donacion donacion,
      EntidadBeneficiaria destinatario,
      Camion camion) {

    this.id = id;
    this.donacion = donacion;
    this.destinatario = destinatario;
    this.camion = camion;
  }

  public long getId() {
    return id;
  }

  public Donacion getDonacion() {
    return donacion;
  }

  public EntidadBeneficiaria getDestinatario() {
    return destinatario;
  }

  public Camion getCamion() {
    return camion;
  }
}
