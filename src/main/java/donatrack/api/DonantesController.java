package donatrack.api;

import donatrack.gestion.GestorPersonas;
import donatrack.model.persona.Persona;

import java.util.List;


public class DonantesController {

  private final GestorPersonas gestor =
      new GestorPersonas();

  // GET /donantes
  public List<Persona> todas() {
    return gestor.todos();
  }

  // GET /donantes/{email}
  public Persona buscar(String email) {
    return gestor.buscar(email);
  }

  // POST /donantes
  public void registrar(String email, Persona persona) {
    gestor.registrar(email, persona);
  }

  // DELETE /donantes/{email}
  public void eliminar(String email) {
    gestor.eliminar(email);
  }
}
