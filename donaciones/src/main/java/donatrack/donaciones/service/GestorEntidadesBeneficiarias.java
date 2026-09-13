package donatrack.donaciones.service;

import donatrack.donaciones.domain.contacto.MedioContacto;
import donatrack.donaciones.domain.contacto.TipoContacto;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.PersonaJuridica;
import donatrack.donaciones.repository.RepositorioEntidades;
import donatrack.donaciones.repository.RepositorioPersonas;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GestorEntidadesBeneficiarias {

  private final RepositorioEntidades repositorioEntidades =
      RepositorioEntidades.getInstance();

  private final RepositorioPersonas repositorioPersonas =
      RepositorioPersonas.getInstance();

  // GET /entidades
  public List<Beneficiaria> todas() {
    return repositorioEntidades.todas();
  }

  // GET /entidades/{id}
  public Beneficiaria buscar(long id) {
    return repositorioEntidades.buscarPorId(id)
        .orElseThrow(() ->
            new IllegalArgumentException(
                "No existe la beneficiaria con id: " + id
            ));
  }

  // DELETE /entidades/{id}
  public void eliminar(long id) {
    buscar(id);
    repositorioEntidades.eliminar(id);
  }

  public void actualizarDireccion(long id, String nuevaDireccion) {
    Beneficiaria beneficiaria = buscar(id);
    beneficiaria.getPersona().setDireccion(nuevaDireccion);
  }

  public Optional<PersonaJuridica> buscarPorCuit(String cuit) {
    return repositorioPersonas.buscarJuridicaPorCuit(cuit);
  }

  public List<String> datosFaltantesParaBeneficiaria(PersonaJuridica juridica) {
    List<String> faltantes = new ArrayList<>();
    if (juridica.getDireccion() == null || juridica.getDireccion().isBlank()) {
      faltantes.add("direccion");
    }
    if (!juridica.tieneContactoDeTipo(TipoContacto.TELEFONO)) {
      faltantes.add("telefono");
    }
    return faltantes;
  }

  public Beneficiaria registrarDePersonaExistente(String cuit, DatosEntidad datos) {
    PersonaJuridica juridica = repositorioPersonas.buscarJuridicaPorCuit(cuit)
        .orElseThrow(() -> new IllegalArgumentException(
            "No existe una persona juridica con CUIT " + cuit
        ));
    completarDatosFaltantes(juridica, datos);
    return crearRol(juridica);
  }

  public Beneficiaria registrarConPersonaNueva(PersonaJuridica juridica, DatosEntidad datos) {
    repositorioPersonas.guardar(juridica);
    completarDatosFaltantes(juridica, datos);
    return crearRol(juridica);
  }

  private void completarDatosFaltantes(PersonaJuridica juridica, DatosEntidad datos) {
    if (datos == null) {
      return;
    }
    if ((juridica.getDireccion() == null || juridica.getDireccion().isBlank())
        && datos.direccion() != null && !datos.direccion().isBlank()) {
      juridica.setDireccion(datos.direccion());
    }
    if (!juridica.tieneContactoDeTipo(TipoContacto.TELEFONO)
        && datos.telefono() != null && !datos.telefono().isBlank()) {
      juridica.agregarMedioContacto(new MedioContacto(TipoContacto.TELEFONO, datos.telefono()));
    }
  }

  private Beneficiaria crearRol(PersonaJuridica juridica) {
    return repositorioEntidades.buscarPorCuit(juridica.getCuit())
        .orElseGet(() -> {
          Beneficiaria beneficiaria = new Beneficiaria(juridica);
          repositorioEntidades.guardar(beneficiaria);
          return beneficiaria;
        });
  }
}
