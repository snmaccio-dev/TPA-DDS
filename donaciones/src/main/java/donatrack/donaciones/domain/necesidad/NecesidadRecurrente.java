package donatrack.donaciones.domain.necesidad;

import donatrack.donaciones.domain.catalogo.Subcategoria;
import jakarta.persistence.*;

@Entity
@DiscriminatorValue("RECURRENTE")
public class NecesidadRecurrente extends Necesidad {

  @Enumerated(EnumType.STRING)
  @Column(name = "periodo")
  private Periodo periodo;

  public NecesidadRecurrente(String descripcion,
                             int cantidad,
                             Subcategoria subcategoria,
                             Periodo periodo) {
    super(descripcion, cantidad, subcategoria);
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
