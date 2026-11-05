package donatrack.logistica.domain.almacenamiento;

import donatrack.logistica.domain.DatosInvalidosException;

import java.util.List;

public record FotoParaSubir(String nombre, byte[] contenido) {

  private static final long MAXIMO_BYTES = 5L * 1024 * 1024;
  private static final List<String> EXTENSIONES_ADMITIDAS =
      List.of(".jpg", ".jpeg", ".png", ".webp");

  public FotoParaSubir {
    if (nombre == null || nombre.isBlank()) {
      throw new DatosInvalidosException("La foto debe tener un nombre de archivo.");
    }
    if (contenido == null || contenido.length == 0) {
      throw new DatosInvalidosException("La foto '" + nombre + "' esta vacia.");
    }
    if (contenido.length > MAXIMO_BYTES) {
      throw new DatosInvalidosException(
          "La foto '" + nombre + "' supera los 5 MB permitidos."
      );
    }
    if (EXTENSIONES_ADMITIDAS.stream().noneMatch(nombre.toLowerCase()::endsWith)) {
      throw new DatosInvalidosException(
          "La foto '" + nombre + "' debe ser jpg, jpeg, png o webp."
      );
    }
  }

  public String extension() {
    return nombre.substring(nombre.lastIndexOf(".")).toLowerCase();
  }
}
