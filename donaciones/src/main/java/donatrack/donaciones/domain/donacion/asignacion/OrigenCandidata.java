package donatrack.donaciones.domain.donacion.asignacion;

// De que ejecucion sale una candidata de la propuesta. AMBOS = coincidencia entre los dos
// algoritmos: en ese caso la propuesta solo guarda las coincidencias.
public enum OrigenCandidata {

  AMBOS("Coinciden ambos algoritmos"),
  COMPATIBILIDAD_SEMANTICA("Compatibilidad semántica"),
  PRIORIDAD_SUBATENDIDOS("Prioridad a sub-atendidos");

  private final String nombre;

  OrigenCandidata(String nombre) {
    this.nombre = nombre;
  }

  public String getNombre() {
    return nombre;
  }
}
