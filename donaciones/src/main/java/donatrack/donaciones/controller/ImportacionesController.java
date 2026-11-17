package donatrack.donaciones.controller;

import donatrack.donaciones.controller.dto.ImportacionCsvDTO;
import donatrack.donaciones.domain.DatosInvalidosException;
import donatrack.donaciones.service.ImportacionDonantesService;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;

import java.util.Locale;

public class ImportacionesController {

  private final ImportacionDonantesService service;

  public ImportacionesController(ImportacionDonantesService service) {
    this.service = service;
  }

  public void iniciar(Context ctx) {
    UploadedFile archivo = ctx.uploadedFile("archivo");
    if (archivo == null) {
      throw new DatosInvalidosException("Hay que enviar el archivo CSV en el campo 'archivo'.");
    }
    String nombre = archivo.filename();
    if (nombre == null || !nombre.toLowerCase(Locale.ROOT).endsWith(".csv")) {
      throw new DatosInvalidosException("El archivo tiene que tener extension .csv.");
    }
    long id = service.iniciar(nombre, archivo.content());
    ctx.status(202).json(new ImportacionIniciadaResponse(id));
  }

  public void buscar(Context ctx) {
    long id = ctx.pathParamAsClass("id", Long.class).get();
    ctx.json(ImportacionCsvDTO.conErrores(service.buscar(id)));
  }

  public void ultimas(Context ctx) {
    ctx.json(ImportacionCsvDTO.resumenes(service.ultimas()));
  }

  public record ImportacionIniciadaResponse(long importacionId) {}
}
