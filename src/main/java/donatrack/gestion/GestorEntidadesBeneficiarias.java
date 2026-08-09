package donatrack.gestion;

import donatrack.model.entidad.EntidadBeneficiaria;
import donatrack.repositorio.RepositorioEntidades;

import java.util.List;

public class GestorEntidadesBeneficiarias {

  private final RepositorioEntidades repositorio =
      RepositorioEntidades.getInstance();

  // POST /entidades
  public EntidadBeneficiaria crear(
      EntidadBeneficiaria entidad
  ) {
    repositorio.guardar(
        entidad.getRazonSocial(),
        entidad
    );

    return entidad;
  }

  // GET /entidades
  public List<EntidadBeneficiaria> todas() {
    return repositorio.todas();
  }

  // GET /entidades/{razonSocial}
  public EntidadBeneficiaria buscar(String razonSocial) {
    return repositorio.buscarPorRazonSocial(razonSocial)
        .orElseThrow(() ->
            new IllegalArgumentException(
                "No existe la entidad beneficiaria: "
                    + razonSocial
            ));
  }

  // DELETE /entidades/{razonSocial}
  public void eliminar(String razonSocial) {
    buscar(razonSocial);
    repositorio.eliminar(razonSocial);
  }

  public void actualizar(
      String razonSocial,
      EntidadBeneficiaria nuevosDatos
  ) {
    EntidadBeneficiaria existente = buscar(razonSocial);

    existente.setDireccion(nuevosDatos.getDireccion());

    // Actualizar demas datos recordatorio
  }
}