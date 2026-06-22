package donatrack.importacion;

import donatrack.model.contacto.TipoContacto;
import donatrack.model.persona.Persona;
import donatrack.repositorio.RepositorioPersonas;

public class ImportadorCSVPersonas extends ImportadorCSV<Persona> {

  @Override
  protected Persona procesarFila(String[] campos) {
    return PersonaFactory.crear(campos);
  }

  public void importarConResumen(String rutaArchivo) {
    int cantidad = importar(rutaArchivo).size();

    System.out.println("[CSV] Importacion finalizada. Registros importados: "
        + cantidad);
  }
}
