package donatrack.model.necesidad;

import donatrack.model.catalogo.Subcategoria;

public class NecesidadOrdinaria extends Necesidad {

  public NecesidadOrdinaria(String descripcion, int cantidad, Subcategoria subcategoria) {
    super(descripcion, cantidad, subcategoria);
  }

  @Override
  public boolean esExtraordinaria() {
    return false;
  }
}