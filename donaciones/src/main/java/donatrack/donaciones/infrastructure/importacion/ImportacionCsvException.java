package donatrack.donaciones.infrastructure.importacion;

public class ImportacionCsvException extends RuntimeException {

  public ImportacionCsvException(String mensaje, Throwable causa) {
    super(mensaje, causa);
  }
}
