package donatrack.logistica.service;

import donatrack.logistica.contrato.EventoEntrega;
import donatrack.logistica.contrato.EventoInicioRuta;
import donatrack.logistica.contrato.EventoRetornoDeposito;
import donatrack.logistica.contrato.ResultadoEntrega;
import donatrack.logistica.domain.entrega.Entrega;
import donatrack.logistica.domain.flota.Chofer;
import donatrack.logistica.domain.integracion.ClienteDonaciones;
import donatrack.logistica.domain.ruta.RutaReparto;
import donatrack.logistica.repository.RepositorioEntregas;
import donatrack.logistica.repository.RepositorioRutas;

import java.util.List;

public class GestorEntregas {

  private final ClienteDonaciones clienteDonaciones;
  private final String urlBaseMonitoreo;

  private final RepositorioEntregas repositorioEntregas =
      RepositorioEntregas.getInstance();
  private final RepositorioRutas repositorioRutas =
      RepositorioRutas.getInstance();

  public GestorEntregas(ClienteDonaciones clienteDonaciones, String urlBaseMonitoreo) {
    if (clienteDonaciones == null) {
      throw new IllegalArgumentException("Debe indicarse el cliente del servicio de Donaciones.");
    }
    if (urlBaseMonitoreo == null || urlBaseMonitoreo.isBlank()) {
      throw new IllegalArgumentException("Debe indicarse la URL base del monitoreo.");
    }
    this.clienteDonaciones = clienteDonaciones;
    this.urlBaseMonitoreo = urlBaseMonitoreo;
  }

  public RutaReparto iniciarRuta(long rutaId, Chofer chofer) {
    RutaReparto ruta = buscarRuta(rutaId);
    List<Long> donacionIds = ruta.iniciarRuta(chofer);

    clienteDonaciones.publicarInicioRuta(
        new EventoInicioRuta(ruta.getId(), donacionIds, linkMapaDe(ruta))
    );

    return ruta;
  }

  public Entrega confirmarRecepcion(long entregaId, List<String> fotos) {
    Entrega entrega = buscar(entregaId);
    entrega.confirmarRecepcion(fotos);

    publicarResultado(entrega, ResultadoEntrega.ENTREGADA);
    cerrarRutaSiTermino(entrega);

    return entrega;
  }

  public Entrega marcarNoRecibida(long entregaId, String motivo) {
    Entrega entrega = buscar(entregaId);
    entrega.marcarNoRecibida(motivo);

    publicarResultado(entrega, ResultadoEntrega.NO_RECIBIDA);
    cerrarRutaSiTermino(entrega);

    return entrega;
  }

  public Entrega retornarADeposito(long entregaId) {
    Entrega entrega = buscar(entregaId);
    entrega.retornarADeposito();

    clienteDonaciones.publicarRetornoADeposito(
        new EventoRetornoDeposito(entrega.getDonacionId())
    );

    return entrega;
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

  public List<Entrega> deRuta(long rutaId) {
    return buscarRuta(rutaId).getEntregas();
  }

  private void publicarResultado(Entrega entrega, ResultadoEntrega resultado) {
    clienteDonaciones.publicarEntrega(
        EventoEntrega.de(
            entrega.getDonacionId(),
            resultado,
            entrega.getFechaHoraEntrega(),
            entrega.getPatenteCamion(),
            entrega.getFotos(),
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
