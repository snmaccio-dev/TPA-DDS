package donatrack.web.integracion;

import com.fasterxml.jackson.core.type.TypeReference;
import donatrack.web.vista.CamionVista;
import donatrack.web.vista.EntregaVista;
import donatrack.web.vista.MonitoreoRutaVista;
import donatrack.web.vista.RutaVista;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ClienteLogisticaHttp implements ClienteLogistica {

  private static final String SERVICIO = "Logística";

  private final ClienteHttp http;

  public ClienteLogisticaHttp(String urlBase) {
    this.http = new ClienteHttp(urlBase, SERVICIO);
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

  @Override
  public List<RutaVista> rutas(String autorizacion) {
    return http.obtener(autorizacion, "/rutas", new TypeReference<List<RutaVista>>() {});
  }

  @Override
  public List<EntregaVista> entregasPendientesDePlanificacion(String autorizacion) {
    return http.obtener(
        autorizacion,
        "/planificacion/pendientes",
        new TypeReference<List<EntregaVista>>() {}
    );
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
