package donatrack.model.necesidad;

import donatrack.model.catalogo.Subcategoria;

public class NecesidadRecurrente extends Necesidad {

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
