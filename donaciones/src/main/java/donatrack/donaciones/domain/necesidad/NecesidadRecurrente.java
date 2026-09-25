package donatrack.donaciones.domain.necesidad;

import donatrack.donaciones.domain.catalogo.Subcategoria;
import donatrack.donaciones.domain.persona.Beneficiaria;
import javax.persistence.*;

@Entity
@DiscriminatorValue("RECURRENTE")
public class NecesidadRecurrente extends Necesidad {

  @Enumerated(EnumType.STRING)
  @Column(name = "periodo")
  private Periodo periodo;

  protected NecesidadRecurrente() {
  }

  public NecesidadRecurrente(String descripcion,
                             int cantidad,
                             Subcategoria subcategoria,
                             Beneficiaria entidad,
                             Periodo periodo) {
    super(descripcion, cantidad, subcategoria, entidad);
    this.periodo = periodo;
  }

  public Periodo getPeriodo() {
    return periodo;
  }

  public void setPeriodo(Periodo periodo) {
    this.periodo = periodo;
  }

  @Override
  public boolean esExtraordinaria() {
    return false;
  }
}
