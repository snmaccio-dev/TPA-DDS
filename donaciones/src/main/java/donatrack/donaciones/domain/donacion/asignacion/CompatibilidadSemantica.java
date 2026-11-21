package donatrack.donaciones.domain.donacion.asignacion;

import java.util.Comparator;
import java.util.List;

import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.persona.Beneficiaria;

// Ordena por correspondencia entre el bien donado y las necesidades declaradas: el
// puntaje es la cantidad pedida en las necesidades de esa subcategoria. Una entidad con
// puntaje 0 no corresponde al bien y queda afuera. A igual puntaje, por id (estable).
public class CompatibilidadSemantica implements Algoritmo {

  @Override
  public List<Beneficiaria> matchmaking(
      Donacion donacion,
      List<Beneficiaria> candidatas) {

    Comparator<Beneficiaria> porPuntaje = Comparator.comparingInt(
        (Beneficiaria beneficiaria) -> puntaje(beneficiaria, donacion)).reversed();

    return candidatas.stream()
        .filter(beneficiaria -> puntaje(beneficiaria, donacion) > 0)
        .sorted(porPuntaje.thenComparing(Desempate.POR_ID))
        .limit(MAXIMO_DE_CANDIDATAS)
        .toList();
  }

  private static int puntaje(Beneficiaria beneficiaria, Donacion donacion) {
    return beneficiaria.cantidadNecesitadaDe(donacion.getSubcategoria());
  }
}
