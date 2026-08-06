package donatrack.model.logistica;

import java.util.List;

public class RutaReparto {

  private Camion camion;
  private List<DestinoEntrega> destinos;

  public RutaReparto(Camion camion, List<DestinoEntrega> destinos) {
    this.camion = camion;
    this.destinos = destinos;
  }

  public Camion getCamion() {
    return camion;
  }

  public List<DestinoEntrega> getDestinos() {
    return destinos;
  }
}