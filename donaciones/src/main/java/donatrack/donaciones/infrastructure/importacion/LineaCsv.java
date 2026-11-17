package donatrack.donaciones.infrastructure.importacion;

public sealed interface LineaCsv permits FilaDonanteCsv, LineaInvalida {

  int numero();
}
