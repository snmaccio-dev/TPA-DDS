package donatrack.web.integracion;

import com.fasterxml.jackson.core.type.TypeReference;
import donatrack.web.vista.CamionVista;
import donatrack.web.vista.EntregaPublicaVista;
import donatrack.web.vista.EntregaVista;
import donatrack.web.integracion.graphql.ConsultaGraphQl;
import donatrack.web.integracion.graphql.RespuestaDePlanificacion;
import donatrack.web.vista.MonitoreoRutaVista;
import donatrack.web.vista.PlanificacionVista;
import donatrack.web.vista.RutaVista;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class ClienteLogisticaHttp implements ClienteLogistica {

  private static final String SERVICIO = "Logística";

  private final ClienteHttp http;

  public ClienteLogisticaHttp(String urlBase) {
    this.http = new ClienteHttp(urlBase, SERVICIO);
  }

  @Override
  public List<EntregaPublicaVista> entregasPublicas() {
    return http.obtener(null, "/entregas/publicas",
        new TypeReference<List<EntregaPublicaVista>>() {});
  }

  @Override
  public List<CamionVista> camiones(String autorizacion) {
    return http.obtener(autorizacion, "/camiones", new TypeReference<List<CamionVista>>() {});
  }

  @Override
  public CamionVista camion(String autorizacion, String patente) {
    return http.obtener(autorizacion, rutaDeCamion(patente), new TypeReference<CamionVista>() {});
  }

  @Override
  public void crearCamion(String autorizacion, NuevoCamion camion) {
    http.publicar(autorizacion, "/camiones", camion);
  }

  @Override
  public void actualizarCamion(String autorizacion,
                               String patente,
                               CapacidadesDeCamion capacidades) {
    http.reemplazar(autorizacion, rutaDeCamion(patente), capacidades);
  }

  @Override
  public List<MonitoreoRutaVista> monitoreoDeRutasEnCurso(String autorizacion) {
    return http.obtener(
        autorizacion,
        "/monitoreo/rutas",
        new TypeReference<List<MonitoreoRutaVista>>() {}
    );
  }

  // Una sola consulta en lugar de GET /rutas mas GET /planificacion/pendientes, y de
  // cada entrega de un destino se pide solo el id, que es lo unico que la pantalla usa
  // para contarlas. Por REST venia el DTO completo de cada una.
  private static final String CONSULTA_DE_PLANIFICACION = "{ planificacion {"
      + " rutas { id patenteCamion estado chofer fechaHoraInicio porcentajeAvance"
      + " destinos { direccion entregas { id } } }"
      + " pendientes { id donacionId descripcionDonacion cantidad unidad direccionDestino }"
      + " } }";

  @Override
  public PlanificacionVista planificacion(String autorizacion) {
    RespuestaDePlanificacion respuesta = http.consultar(
        autorizacion,
        "/graphql",
        new ConsultaGraphQl(CONSULTA_DE_PLANIFICACION),
        new TypeReference<RespuestaDePlanificacion>() {}
    );

    // GraphQL responde 200 aunque la consulta haya fallado: el error viaja en el cuerpo.
    if (respuesta.tieneErrores()) {
      throw new ServicioNoDisponibleException(
          SERVICIO, "devolvió un error en la consulta: " + respuesta.primerError()
      );
    }

    return respuesta.data().planificacion();
  }

  @Override
  public List<RutaVista> rutasDelChofer(String autorizacion) {
    return http.obtener(autorizacion, "/rutas/mias", new TypeReference<List<RutaVista>>() {});
  }

  @Override
  public void iniciarRuta(String autorizacion, long rutaId) {
    // Sin cuerpo: Logistica deduce que chofer inicia la ruta a partir del token.
    http.publicar(autorizacion, "/rutas/" + rutaId + "/inicio", Map.of());
  }

  @Override
  public EntregaVista entrega(String autorizacion, long entregaId) {
    return http.obtener(
        autorizacion,
        "/entregas/" + entregaId,
        new TypeReference<EntregaVista>() {}
    );
  }

  @Override
  public List<String> subirFotos(String autorizacion,
                                 long entregaId,
                                 List<ArchivoSubido> fotos) {
    return http.publicarArchivos(
        autorizacion,
        "/entregas/" + entregaId + "/fotos",
        "fotos",
        fotos,
        new TypeReference<FotosSubidas>() {}
    ).urlsOVacio();
  }

  @Override
  public void confirmarRecepcion(String autorizacion, long entregaId, List<String> fotos) {
    http.publicar(
        autorizacion,
        "/entregas/" + entregaId + "/confirmacion",
        new ConfirmarRecepcion(fotos)
    );
  }

  private String rutaDeCamion(String patente) {
    return "/camiones/"
        + URLEncoder.encode(patente, StandardCharsets.UTF_8).replace("+", "%20");
  }
}
