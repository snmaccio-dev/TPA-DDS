package donatrack.logistica.infrastructure.almacenamiento;

import donatrack.logistica.domain.almacenamiento.AlmacenDeFotos;
import donatrack.logistica.domain.almacenamiento.FotoParaSubir;
import donatrack.logistica.domain.integracion.IntegracionExternaException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

public class AlmacenEnDisco implements AlmacenDeFotos {

  private final Path directorio;
  private final String urlBase;

  public AlmacenEnDisco(String directorio, String urlBase) {
    Objects.requireNonNull(directorio, "Debe indicarse el directorio de fotos.");
    Objects.requireNonNull(urlBase, "Debe indicarse la URL base de las fotos.");

    this.directorio = Path.of(directorio).toAbsolutePath();
    this.urlBase = urlBase.endsWith("/")
        ? urlBase.substring(0, urlBase.length() - 1)
        : urlBase;

    try {
      Files.createDirectories(this.directorio);
    } catch (IOException e) {
      throw new IntegracionExternaException(
          "No se pudo preparar el directorio de fotos " + this.directorio + ".", e
      );
    }
  }

  @Override
  public String guardar(FotoParaSubir foto) {
    String nombre = UUID.randomUUID() + foto.extension();

    try {
      Files.write(directorio.resolve(nombre), foto.contenido());
    } catch (IOException e) {
      throw new IntegracionExternaException(
          "No se pudo guardar la foto " + foto.nombre() + ".", e
      );
    }

    return urlBase + "/" + nombre;
  }

  public Path getDirectorio() {
    return directorio;
  }
}
