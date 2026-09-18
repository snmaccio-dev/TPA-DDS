package donatrack.logistica.service;

import donatrack.logistica.domain.flota.Chofer;
import donatrack.logistica.repository.RepositorioChoferes;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;

import java.util.List;

public class GestorChoferes implements WithSimplePersistenceUnit {

  private final RepositorioChoferes repositorio =
      RepositorioChoferes.getInstance();

  public List<Chofer> todos() {
    return repositorio.todos();
  }

  public Chofer buscar(long id) {
    return repositorio.buscarPorId(id)
        .orElseThrow(() ->
            new RecursoInexistenteException(
                "No existe el chofer con ID " + id
            ));
  }

  public Chofer crear(Chofer chofer) {
    if (repositorio.buscarPorDocumento(chofer.getDocumento()).isPresent()) {
      throw new IllegalStateException(
          "Ya existe un chofer con documento " + chofer.getDocumento() + "."
      );
    }

    withTransaction(() -> repositorio.guardar(chofer));
    return chofer;
  }

  public void eliminar(long id) {
    withTransaction(() -> repositorio.eliminar(id));
  }
}
