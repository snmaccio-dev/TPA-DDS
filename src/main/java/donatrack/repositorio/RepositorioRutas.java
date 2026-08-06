package donatrack.repositorio;

import donatrack.model.logistica.RutaReparto;

import java.util.ArrayList;
import java.util.List;

public class RepositorioRutas {

  private List<RutaReparto> rutas = new ArrayList<>();

  public void guardar(List<RutaReparto> nuevasRutas) {
    rutas.addAll(nuevasRutas);
  }


  public List<RutaReparto> obtenerTodas() {
    return rutas;
  }
}
