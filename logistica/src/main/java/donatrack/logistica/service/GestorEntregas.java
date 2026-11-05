package donatrack.logistica.service;

import donatrack.logistica.contrato.EventoEntrega;
import donatrack.logistica.contrato.EventoInicioRuta;
import donatrack.logistica.contrato.EventoRetornoDeposito;
import donatrack.logistica.contrato.ResultadoEntrega;
import donatrack.logistica.domain.DatosInvalidosException;
import donatrack.logistica.domain.almacenamiento.AlmacenDeFotos;
import donatrack.logistica.domain.almacenamiento.FotoParaSubir;
import donatrack.logistica.domain.entrega.Entrega;
import donatrack.logistica.domain.flota.Chofer;
import donatrack.logistica.domain.integracion.ClienteDonaciones;
import donatrack.logistica.domain.ruta.RutaReparto;
import donatrack.logistica.repository.RepositorioChoferes;
import donatrack.logistica.repository.RepositorioEntregas;
import donatrack.logistica.repository.RepositorioRutas;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;

import java.util.List;
import java.util.Objects;

public class GestorEntregas implements WithSimplePersistenceUnit {

  private final ClienteDonaciones clienteDonaciones;
  private final String urlBaseMonitoreo;
  private final AlmacenDeFotos almacenDeFotos;

  private final RepositorioEntregas repositorioEntregas =
      RepositorioEntregas.getInstance();
  private final RepositorioRutas repositorioRutas =
      RepositorioRutas.getInstance();
  private final RepositorioChoferes repositorioChoferes =
      RepositorioChoferes.getInstance();

  public GestorEntregas(ClienteDonaciones clienteDonaciones,
                        String urlBaseMonitoreo,
                        AlmacenDeFotos almacenDeFotos) {
    Objects.requireNonNull(
        clienteDonaciones, "Debe indicarse el cliente del servicio de Donaciones.");
    Objects.requireNonNull(almacenDeFotos, "Debe indicarse el almacen de fotos.");
    if (urlBaseMonitoreo == null || urlBaseMonitoreo.isBlank()) {
      throw new IllegalArgumentException("Debe indicarse la URL base del monitoreo.");
    }
    this.clienteDonaciones = clienteDonaciones;
    this.urlBaseMonitoreo = urlBaseMonitoreo;
    this.almacenDeFotos = almacenDeFotos;
  }

  public List<String> subirFotos(long entregaId, List<FotoParaSubir> fotos) {
    buscar(entregaId);

    if (fotos == null || fotos.isEmpty()) {
      throw new DatosInvalidosException("Debe adjuntarse al menos una foto.");
    }

    return fotos.stream().map(almacenDeFotos::guardar).toList();
  }

  public RutaReparto iniciarRuta(long rutaId, long choferId) {
    return withTransaction(() -> {
      RutaReparto ruta = buscarRuta(rutaId);
      List<Long> donacionIds = ruta.iniciarRuta(buscarChofer(choferId));

      clienteDonaciones.publicarInicioRuta(
          new EventoInicioRuta(ruta.getId(), donacionIds, linkMapaDe(ruta))
      );

      return ruta;
    });
  }

  public Entrega confirmarRecepcion(long entregaId, List<String> fotos) {
    return withTransaction(() -> {
      Entrega entrega = buscar(entregaId);
      entrega.confirmarRecepcion(fotos);

      publicarResultado(entrega, ResultadoEntrega.ENTREGADA);
      cerrarRutaSiTermino(entrega);

      return entrega;
    });
  }

  public Entrega marcarNoRecibida(long entregaId, String motivo) {
    return withTransaction(() -> {
      Entrega entrega = buscar(entregaId);
      entrega.marcarNoRecibida(motivo);

      publicarResultado(entrega, ResultadoEntrega.NO_RECIBIDA);
      cerrarRutaSiTermino(entrega);

      return entrega;
    });
  }

  public Entrega retornarADeposito(long entregaId) {
    return withTransaction(() -> {
      Entrega entrega = buscar(entregaId);
      entrega.retornarADeposito();

      clienteDonaciones.publicarRetornoADeposito(
          new EventoRetornoDeposito(entrega.getDonacionId())
      );

      return entrega;
    });
  }

  public Entrega registrarMedicion(long entregaId, double pesoKg, double volumenM3) {
    return withTransaction(() -> {
      Entrega entrega = buscar(entregaId);
      entrega.registrarMedicion(pesoKg, volumenM3);
      return entrega;
    });
  }

  public List<Entrega> pendientesDeMedicion() {
    return repositorioEntregas.pendientesDeMedicion();
  }

  public Entrega buscar(long entregaId) {
    return repositorioEntregas.buscarPorId(entregaId)
        .orElseThrow(() ->
            new RecursoInexistenteException(
                "No existe la entrega con ID " + entregaId
            ));
  }

  public List<Entrega> todas() {
    return repositorioEntregas.todas();
  }

  public List<Entrega> entregadas() {
    return repositorioEntregas.entregadas();
  }

  public List<Entrega> deRuta(long rutaId) {
    return buscarRuta(rutaId).getEntregas();
  }

  private void publicarResultado(Entrega entrega, ResultadoEntrega resultado) {
    // La patente solo se manda si la entrega efectivamente se concretó. En NO_RECIBIDA la
    // entrega todavia tiene patente cargada (se limpia recien en retornarADeposito), asi
    // que hay que omitirla explicitamente.
    String patente = resultado == ResultadoEntrega.ENTREGADA
        ? entrega.getPatenteCamion()
        : null;

    // Esta llamada ocurre dentro de la transaccion del caso de uso: si Donaciones no
    // responde, se hace rollback y el cambio local no se confirma. La contrapartida es
    // que la transaccion queda abierta durante una llamada de red.
    clienteDonaciones.publicarEntrega(
        EventoEntrega.de(
            entrega.getDonacionId(),
            resultado,
            entrega.getFechaHoraEntrega(),
            patente,
            entrega.getMotivoNoRecepcion()
        )
    );
  }

  private void cerrarRutaSiTermino(Entrega entrega) {
    RutaReparto ruta = entrega.getRuta();
    if (ruta != null) {
      ruta.finalizarSiNoQuedanEntregasEnTraslado();
    }
  }

  private Chofer buscarChofer(long choferId) {
    return repositorioChoferes.buscarPorId(choferId)
        .orElseThrow(() ->
            new RecursoInexistenteException(
                "No existe el chofer con ID " + choferId
            ));
  }

  private RutaReparto buscarRuta(long rutaId) {
    return repositorioRutas.buscarPorId(rutaId)
        .orElseThrow(() ->
            new RecursoInexistenteException(
                "No existe una ruta con ID " + rutaId
            ));
  }

  private String linkMapaDe(RutaReparto ruta) {
    return urlBaseMonitoreo + "/monitoreo/rutas/" + ruta.getId();
  }
}
