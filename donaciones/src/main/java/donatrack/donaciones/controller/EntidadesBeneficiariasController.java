package donatrack.donaciones.controller;

import donatrack.donaciones.service.DatosEntidad;
import donatrack.donaciones.service.GestorEntidadesBeneficiarias;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.PersonaJuridica;

import java.util.List;
import java.util.Optional;

public class EntidadesBeneficiariasController {

  private final GestorEntidadesBeneficiarias gestor =
      new GestorEntidadesBeneficiarias();

  public record ConsultaCuit(boolean existePersona, List<String> datosFaltantes) {}

  // GET /entidades
  public List<Beneficiaria> todas() {
    return gestor.todas();
  }

  // GET /entidades/{id}
  public Beneficiaria buscar(long id) {
    return gestor.buscar(id);
  }

  // GET /entidades/consulta?cuit=...
  public ConsultaCuit consultarPorCuit(String cuit) {
    Optional<PersonaJuridica> encontrada = gestor.buscarPorCuit(cuit);
    if (encontrada.isPresent()) {
      return new ConsultaCuit(true, gestor.datosFaltantesParaBeneficiaria(encontrada.get()));
    }
    return new ConsultaCuit(false, List.of("direccion", "telefono"));
  }

  // POST /entidades/existente  { cuit, direccion?, telefono? }
  public Beneficiaria altaDePersonaExistente(String cuit, DatosEntidad datos) {
    return gestor.registrarDePersonaExistente(cuit, datos);
  }

  // POST /entidades/nueva  { juridica, direccion?, telefono? }
  public Beneficiaria altaConPersonaNueva(PersonaJuridica juridica, DatosEntidad datos) {
    return gestor.registrarConPersonaNueva(juridica, datos);
  }

  // DELETE /entidades/{id}
  public void eliminar(long id) {
    gestor.eliminar(id);
  }
}
