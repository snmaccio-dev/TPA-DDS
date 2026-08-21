package donatrack.importacion;

import donatrack.model.persona.Donante;
import donatrack.model.persona.Persona;
import donatrack.repositorio.RepositorioPersonas;

public class ImportadorCSVPersonas extends ImportadorCSV<Persona> {

  private final RepositorioPersonas repositorio;
  private int creados = 0;
  private int actualizados = 0;

  public ImportadorCSVPersonas() {
    this(RepositorioPersonas.getInstance());
  }

  public ImportadorCSVPersonas(RepositorioPersonas repositorio) {
    this.repositorio = repositorio;
  }

  @Override
  protected Persona procesarFila(String[] campos) {
    String email = PersonaFactory.extraerEmail(campos);
    Persona persona = PersonaFactory.crear(campos);

    if (repositorio.existe(email)) {
      actualizados++;
    } else {
      creados++;
    }

    new Donante(persona);

    repositorio.guardar(email, persona);

    return persona;
  }

  public void importarConResumen(String rutaArchivo) {
    creados = 0;
    actualizados = 0;
    int total = importar(rutaArchivo).size();

    System.out.println("[CSV] Importacion finalizada. Total: " + total
        + " (creados: " + creados
        + ", actualizados: " + actualizados + ")");
  }
}
