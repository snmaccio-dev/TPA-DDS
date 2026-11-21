package donatrack.donaciones.service;

import donatrack.donaciones.domain.geocodificacion.Geocodificador;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.Coordenadas;
import donatrack.donaciones.repository.RepositorioEntidades;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

// Coordenadas de las entidades beneficiarias para el mapa publico de entregas. Se calculan
// una sola vez, la primera vez que se le confirma una entrega a la entidad, y quedan
// guardadas en la beneficiaria.
//
// La geocodificacion corre en el executor (un solo hilo en la app), asi el evento de
// entrega de Logistica responde sin esperar al proveedor externo, y las consultas quedan en
// fila (Nominatim admite una por segundo). Si falla o no encuentra la direccion, se loguea
// un aviso y la entidad queda sin coordenadas: se vuelve a intentar con la proxima entrega
// confirmada.
public class UbicacionesService implements WithSimplePersistenceUnit {

  private static final Logger LOG = LoggerFactory.getLogger(UbicacionesService.class);

  private final RepositorioEntidades repositorioEntidades;
  private final Geocodificador geocodificador;
  private final Executor executor;

  public UbicacionesService(RepositorioEntidades repositorioEntidades,
                            Geocodificador geocodificador,
                            Executor executor) {
    this.repositorioEntidades = repositorioEntidades;
    this.geocodificador = geocodificador;
    this.executor = executor;
  }

  // No lanza nada: lo llama el evento de entrega y una falla aca no puede hacerlo fallar.
  public void ubicarSiHaceFalta(long beneficiariaId) {
    try {
      executor.execute(() -> ubicarEnSegundoPlano(beneficiariaId));
    } catch (RejectedExecutionException e) {
      LOG.warn("No se pudo encolar la geocodificacion de la beneficiaria {}: {}",
          beneficiariaId, e.getMessage());
    }
  }

  private void ubicarEnSegundoPlano(long beneficiariaId) {
    try {
      ubicar(beneficiariaId);
    } catch (RuntimeException e) {
      LOG.warn("No se pudo geocodificar la direccion de la beneficiaria {}: {}. "
          + "Se reintenta con la proxima entrega confirmada.", beneficiariaId, e.getMessage());
    } finally {
      // El hilo del executor vive mientras viva la app: sin esto, la proxima tarea veria
      // las entidades cacheadas de esta.
      entityManager().clear();
    }
  }

  private void ubicar(long beneficiariaId) {
    // Datos frescos: otra tarea, o un alta con coordenadas, pudo haberla ubicado ya.
    entityManager().clear();
    Optional<Beneficiaria> encontrada = repositorioEntidades.buscarPorId(beneficiariaId);
    if (encontrada.isEmpty() || encontrada.get().tieneUbicacion()) {
      return;
    }
    String direccion = encontrada.get().getPersona().getDireccion();
    if (direccion == null || direccion.isBlank()) {
      LOG.warn("La beneficiaria {} no tiene direccion: queda sin coordenadas.", beneficiariaId);
      return;
    }

    // Fuera de la transaccion: la consulta externa puede tardar.
    Optional<Coordenadas> coordenadas = geocodificador.geocodificar(direccion);
    if (coordenadas.isEmpty()) {
      LOG.warn("El geocodificador no encontro la direccion '{}' de la beneficiaria {}. "
          + "Se reintenta con la proxima entrega confirmada.", direccion, beneficiariaId);
      return;
    }

    withTransaction(() -> {
      entityManager().clear();
      repositorioEntidades.buscarPorId(beneficiariaId)
          // Si mientras tanto cambio la direccion, estas coordenadas ya no le corresponden.
          .filter(b -> !b.tieneUbicacion() && direccion.equals(b.getPersona().getDireccion()))
          .ifPresent(b -> b.ubicarEn(coordenadas.get()));
    });
  }
}
