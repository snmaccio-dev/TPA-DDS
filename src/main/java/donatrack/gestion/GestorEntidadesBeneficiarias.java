package donatrack.gestion;

import donatrack.model.persona.Beneficiaria;
import donatrack.repositorio.RepositorioEntidades;

import java.util.List;

public class GestorEntidadesBeneficiarias {

  private final RepositorioEntidades repositorio =
      RepositorioEntidades.getInstance();

  // POST /entidades
  public Beneficiaria crear(Beneficiaria beneficiaria) {
    repositorio.guardar(beneficiaria);
    return beneficiaria;
  }

  // GET /entidades
  public List<Beneficiaria> todas() {
    return repositorio.todas();
  }

  // GET /entidades/{id}
  public Beneficiaria buscar(long id) {
    return repositorio.buscarPorId(id)
        .orElseThrow(() ->
            new IllegalArgumentException(
                "No existe la beneficiaria con id: " + id
            ));
  }

  // DELETE /entidades/{id}
  public void eliminar(long id) {
    buscar(id);
    repositorio.eliminar(id);
  }

  public void actualizarDireccion(long id, String nuevaDireccion) {
    Beneficiaria beneficiaria = buscar(id);
    beneficiaria.getPersona().setDireccion(nuevaDireccion);
  }
}
