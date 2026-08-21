package donatrack.api;

import donatrack.gestion.GestorEntidadesBeneficiarias;
import donatrack.model.persona.Beneficiaria;

import java.util.List;

public class EntidadesBeneficiariasController {

  private final GestorEntidadesBeneficiarias gestor =
      new GestorEntidadesBeneficiarias();

  // GET /entidades
  public List<Beneficiaria> todas() {
    return gestor.todas();
  }

  // GET /entidades/{id}
  public Beneficiaria buscar(long id) {
    return gestor.buscar(id);
  }

  // POST /entidades
  public Beneficiaria crear(Beneficiaria beneficiaria) {
    return gestor.crear(beneficiaria);
  }

  // DELETE /entidades/{id}
  public void eliminar(long id) {
    gestor.eliminar(id);
  }
}
