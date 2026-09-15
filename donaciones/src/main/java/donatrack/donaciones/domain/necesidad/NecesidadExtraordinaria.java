package donatrack.donaciones.domain.necesidad;

import donatrack.donaciones.domain.catalogo.Subcategoria;

public class NecesidadExtraordinaria extends Necesidad {

  public NecesidadExtraordinaria(String descripcion, int cantidad, Subcategoria subcategoria) {
    super(descripcion, cantidad, subcategoria);
  }

  @Override
  public boolean esExtraordinaria() {
    return true;
  }
}