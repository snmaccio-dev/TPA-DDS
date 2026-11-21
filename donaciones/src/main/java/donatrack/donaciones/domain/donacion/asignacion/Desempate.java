package donatrack.donaciones.domain.donacion.asignacion;

import donatrack.donaciones.domain.persona.Beneficiaria;

import java.util.Comparator;

// Desempate comun de los rankings: por id ascendente (las que no tienen id, al final).
// Sin esto el orden de los empates dependeria del orden en que la base devuelve las filas.
final class Desempate {

  static final Comparator<Beneficiaria> POR_ID = Comparator.comparing(
      Beneficiaria::getId, Comparator.nullsLast(Comparator.naturalOrder()));

  private Desempate() {
  }
}
