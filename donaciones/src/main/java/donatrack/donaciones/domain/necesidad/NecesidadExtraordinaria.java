package donatrack.donaciones.domain.necesidad;

import donatrack.donaciones.domain.catalogo.Subcategoria;
import donatrack.donaciones.domain.persona.Beneficiaria;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("EXTRAORDINARIA")
public class NecesidadExtraordinaria extends Necesidad {

  protected NecesidadExtraordinaria() {
  }

  public NecesidadExtraordinaria(String descripcion, int cantidad,
                                 Subcategoria subcategoria, Beneficiaria entidad) {
    super(descripcion, cantidad, subcategoria, entidad);
  }

  @Override
  public boolean esExtraordinaria() {
    return true;
  }
}