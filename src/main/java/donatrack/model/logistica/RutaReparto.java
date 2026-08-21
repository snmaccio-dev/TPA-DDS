package donatrack.model.logistica;

import donatrack.model.donacion.Donacion;

import java.util.List;

public class RutaReparto {

  private static long proximoId = 1;

  private final long id;
  private Camion camion;
  private List<DestinoEntrega> destinos;

  public RutaReparto(
      Camion camion,
      List<DestinoEntrega> destinos) {

    this.id = proximoId++;
    this.camion = camion;
    this.destinos = destinos;
  }

  public long getId() {
    return id;
  }

  public Camion getCamion() {
    return camion;
  }

  public List<DestinoEntrega> getDestinos() {
    return destinos;
  }

  public void iniciarRuta() {
    destinos.stream()
        .flatMap(destino -> destino.getDonaciones().stream())
        .forEach(donacion -> {
          if (donacion.getEntrega() == null) {
            throw new IllegalStateException(
                "La donacion " + donacion.getId() + " no tiene una entrega asociada."
            );
          }
          donacion.getEntrega().iniciarTraslado();
        });
  }
}