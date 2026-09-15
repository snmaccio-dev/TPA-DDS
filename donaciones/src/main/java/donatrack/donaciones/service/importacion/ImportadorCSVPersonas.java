package donatrack.donaciones.service.importacion;

import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.domain.persona.Persona;
import donatrack.donaciones.repository.RepositorioDonantes;
import donatrack.donaciones.repository.RepositorioPersonas;

public class ImportadorCSVPersonas extends ImportadorCSV<Persona> {

  private final RepositorioPersonas repositorio;
  private final RepositorioDonantes repositorioDonantes;
  private int creados = 0;
  private int actualizados = 0;

  public ImportadorCSVPersonas(RepositorioPersonas repositorio,
                               RepositorioDonantes repositorioDonantes) {
    this.repositorio = repositorio;
    this.repositorioDonantes = repositorioDonantes;
  }

  @Override
  protected Persona procesarFila(String[] campos) {
    Persona persona = PersonaFactory.crear(campos);

    if (repositorio.buscarPorDocumento(persona.getDocumento()).isPresent()) {
      actualizados++;
      return persona;
    }
    creados++;

    Donante donante = new Donante(persona);
    repositorio.guardar(persona);
    repositorioDonantes.guardar(donante);

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