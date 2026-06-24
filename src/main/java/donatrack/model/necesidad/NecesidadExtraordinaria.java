package donatrack.model.necesidad;

import donatrack.model.catalogo.Subcategoria;

public class NecesidadExtraordinaria extends Necesidad {

  public NecesidadExtraordinaria(String descripcion, int cantidad, Subcategoria subcategoria) {
    super(descripcion, cantidad, subcategoria);
  }

  @Override
  public boolean esExtraordinaria() {
    return true;
  }
}