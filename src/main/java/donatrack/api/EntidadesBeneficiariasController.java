package donatrack.api;

import donatrack.gestion.GestorEntidadesBeneficiarias;
import donatrack.model.entidad.EntidadBeneficiaria;

import java.util.List;

public class EntidadesBeneficiariasController {

  private final GestorEntidadesBeneficiarias gestor =
      new GestorEntidadesBeneficiarias();

  // GET /entidades
  public List<EntidadBeneficiaria> todas() {
    return gestor.todas();
  }

  // GET /entidades/{razonSocial}
  public EntidadBeneficiaria buscar(String razonSocial) {
    return gestor.buscar(razonSocial);
  }

  // POST /entidades
  public EntidadBeneficiaria crear(
      EntidadBeneficiaria entidad
  ) {
    return gestor.crear(entidad);
  }

  // DELETE /entidades/{razonSocial}
  public void eliminar(String razonSocial) {
    gestor.eliminar(razonSocial);
  }
}