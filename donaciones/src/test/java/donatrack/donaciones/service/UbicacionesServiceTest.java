package donatrack.donaciones.service;

import donatrack.donaciones.DatosDeTest;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.Coordenadas;
import donatrack.donaciones.domain.persona.PersonaJuridica;
import donatrack.donaciones.domain.persona.Representante;
import donatrack.donaciones.domain.persona.TipoOrganizacion;
import donatrack.donaciones.infrastructure.geocodificacion.GeocodificadorSimulado;
import donatrack.donaciones.repository.RepositorioDonaciones;
import donatrack.donaciones.repository.RepositorioDonantes;
import donatrack.donaciones.repository.RepositorioEntidades;
import donatrack.donaciones.repository.RepositorioNecesidades;
import donatrack.donaciones.repository.RepositorioNotificaciones;
import donatrack.donaciones.repository.RepositorioPersonas;
import donatrack.donaciones.repository.RepositorioPropuestas;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

// Geocodificacion de las entidades (primera entrega confirmada) y su relacion con el alta y
// la modificacion de la direccion. El executor corre la tarea en el mismo hilo para poder
// verificar el resultado enseguida; en la app es un hilo aparte.
class UbicacionesServiceTest implements WithSimplePersistenceUnit {

  private static final Executor EN_EL_MISMO_HILO = Runnable::run;

  private final RepositorioPersonas repoPersonas = RepositorioPersonas.getInstance();
  private final RepositorioEntidades repoEntidades = RepositorioEntidades.getInstance();

  @Test
  void unaEntidadSinCoordenadasQuedaUbicadaConLaPrimeraEntregaConfirmada() {
    String direccion = DatosDeTest.textoUnico("Brandsen 805");
    long id = beneficiariaPersistida(direccion, null);
    GeocodificadorSimulado geocodificador =
        new GeocodificadorSimulado().conocer(direccion, -34.6356, -58.3647);

    servicio(geocodificador).ubicarSiHaceFalta(id);

    assertEquals(Optional.of(new Coordenadas(-34.6356, -58.3647)), ubicacionDe(id));
    assertEquals(List.of(direccion), geocodificador.getConsultadas());
  }

  @Test
  void unaEntidadQueYaTieneCoordenadasNoSeGeocodifica() {
    long id = beneficiariaPersistida(DatosDeTest.textoUnico("Calle"), new Coordenadas(-34.6, -58.4));
    GeocodificadorSimulado geocodificador = new GeocodificadorSimulado();

    servicio(geocodificador).ubicarSiHaceFalta(id);

    assertTrue(geocodificador.getConsultadas().isEmpty());
    assertEquals(Optional.of(new Coordenadas(-34.6, -58.4)), ubicacionDe(id));
  }

  @Test
  void siElGeocodificadorFallaNoLanzaYSeReintentaConLaProximaEntrega() {
    String direccion = DatosDeTest.textoUnico("Av. Rivadavia 1864");
    long id = beneficiariaPersistida(direccion, null);
    GeocodificadorSimulado geocodificador = new GeocodificadorSimulado()
        .conocer(direccion, -34.6098, -58.3925)
        .fallar(true);
    UbicacionesService servicio = servicio(geocodificador);

    assertDoesNotThrow(() -> servicio.ubicarSiHaceFalta(id));
    assertEquals(Optional.empty(), ubicacionDe(id));

    geocodificador.fallar(false);
    servicio.ubicarSiHaceFalta(id);

    assertEquals(Optional.of(new Coordenadas(-34.6098, -58.3925)), ubicacionDe(id));
  }

  @Test
  void siNoEncuentraLaDireccionLaEntidadQuedaSinCoordenadas() {
    long id = beneficiariaPersistida(DatosDeTest.textoUnico("Calle inexistente"), null);

    servicio(new GeocodificadorSimulado()).ubicarSiHaceFalta(id);

    assertEquals(Optional.empty(), ubicacionDe(id));
  }

  @Test
  void unaEntidadInexistenteNoRompeNada() {
    assertDoesNotThrow(() ->
        servicio(new GeocodificadorSimulado()).ubicarSiHaceFalta(Long.MAX_VALUE));
  }

  @Test
  void cambiarLaDireccionBorraLasCoordenadas() {
    String email = DatosDeTest.emailUnico("rep");
    long id = beneficiariaPersistida("Brandsen 805", new Coordenadas(-34.6356, -58.3647), email);

    entidades().modificar(id, "Comedor", "ONG", "Comedor", new DatosEntidad(
        "Av. Sarmiento 2601", "1100000000", List.of(new Representante("Rep", "Uno", email))));

    assertEquals(Optional.empty(), ubicacionDe(id));
  }

  @Test
  void modificarSinCambiarLaDireccionConservaLasCoordenadas() {
    String email = DatosDeTest.emailUnico("rep");
    long id = beneficiariaPersistida("Brandsen 805", new Coordenadas(-34.6356, -58.3647), email);

    entidades().modificar(id, "Comedor Renovado", "ONG", "Comedor", new DatosEntidad(
        "Brandsen 805", "1111111111", List.of(new Representante("Rep", "Uno", email))));

    assertEquals(Optional.of(new Coordenadas(-34.6356, -58.3647)), ubicacionDe(id));
  }

  @Test
  void elListadoPublicoSoloTraeLasPedidasQueTienenCoordenadas() {
    long ubicada = beneficiariaPersistida("Calle 1", new Coordenadas(-34.58, -58.39));
    long sinUbicar = beneficiariaPersistida("Calle 2", null);
    long noPedida = beneficiariaPersistida("Calle 3", new Coordenadas(-34.60, -58.43));
    entityManager().clear();

    List<Long> ids = repoEntidades.conUbicacion(List.of(ubicada, sinUbicar)).stream()
        .map(Beneficiaria::getId)
        .toList();

    assertEquals(List.of(ubicada), ids);
    assertFalse(ids.contains(noPedida));
  }

  // === Helpers ===

  private UbicacionesService servicio(GeocodificadorSimulado geocodificador) {
    return new UbicacionesService(repoEntidades, geocodificador, EN_EL_MISMO_HILO);
  }

  private Optional<Coordenadas> ubicacionDe(long id) {
    entityManager().clear();
    return repoEntidades.buscarPorId(id).orElseThrow().getUbicacion();
  }

  private long beneficiariaPersistida(String direccion, Coordenadas ubicacion) {
    return beneficiariaPersistida(direccion, ubicacion, DatosDeTest.emailUnico("rep"));
  }

  private long beneficiariaPersistida(String direccion, Coordenadas ubicacion, String email) {
    long[] id = new long[1];
    withTransaction(() -> {
      PersonaJuridica juridica = new PersonaJuridica(
          DatosDeTest.cuitUnico(), DatosDeTest.textoUnico("Comedor"), TipoOrganizacion.ONG, "Rubro");
      new DatosEntidad(direccion, "1100000000",
          List.of(new Representante("Rep", "Uno", email))).aplicarA(juridica);
      repoPersonas.guardar(juridica);
      Beneficiaria beneficiaria = new Beneficiaria(juridica);
      beneficiaria.ubicarEn(ubicacion);
      repoEntidades.guardar(beneficiaria);
      id[0] = beneficiaria.getId();
    });
    return id[0];
  }

  private EntidadesBeneficiariasService entidades() {
    return new EntidadesBeneficiariasService(
        repoEntidades, repoPersonas, new RepositorioNotificaciones(),
        RepositorioNecesidades.getInstance(), new RepositorioPropuestas(),
        RepositorioDonaciones.getInstance(), new RepositorioDonantes(),
        mock(UsuariosService.class));
  }
}
