package donatrack.donaciones.infrastructure.importacion;

import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.domain.persona.Persona;
import donatrack.donaciones.repository.RepositorioDonantes;
import donatrack.donaciones.repository.RepositorioPersonas;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;

// TODO: Keycloak. El CSV crea Donantes locales pero no da de alta la cuenta en
// Keycloak, asi que los importados no pueden loguearse. Hay que decidir si
// creamos las cuentas una por una durante el import (lento, N llamadas HTTP)
// o si generamos un segundo paso en lote. Ninguna de las dos opciones
// esta implementada por ahora.
public class ImportadorCSVPersonas extends ImportadorCSV<Persona>
    implements WithSimplePersistenceUnit {

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

    withTransaction(() -> {
      Donante donante = new Donante(persona);
      repositorio.guardar(persona);
      repositorioDonantes.guardar(donante);
    });

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
