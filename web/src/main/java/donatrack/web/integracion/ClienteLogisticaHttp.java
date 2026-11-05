package donatrack.web.integracion;

import com.fasterxml.jackson.core.type.TypeReference;
import donatrack.web.vista.CamionVista;
import donatrack.web.vista.EntregaVista;
import donatrack.web.vista.MonitoreoRutaVista;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class ClienteLogisticaHttp implements ClienteLogistica {

  private static final String SERVICIO = "Logística";

  private final ClienteHttp http;

  public ClienteLogisticaHttp(String urlBase) {
    this.http = new ClienteHttp(urlBase, SERVICIO);
  }

  @Override
  public List<CamionVista> camiones() {
    return http.obtener("/camiones", new TypeReference<List<CamionVista>>() {});
  }

  @Override
  public CamionVista camion(String patente) {
    return http.obtener(rutaDeCamion(patente), new TypeReference<CamionVista>() {});
  }

  @Override
  public void crearCamion(NuevoCamion camion) {
    http.publicar("/camiones", camion);
  }

  @Override
  public void actualizarCamion(String patente, CapacidadesDeCamion capacidades) {
    http.reemplazar(rutaDeCamion(patente), capacidades);
  }

  @Override
  public List<MonitoreoRutaVista> monitoreoDeBeneficiaria(long beneficiariaId) {
    return monitoreo("?beneficiaria=" + beneficiariaId);
  }

  @Override
  public List<MonitoreoRutaVista> monitoreoDeDonaciones(List<Long> donacionIds) {
    if (donacionIds == null || donacionIds.isEmpty()) {
      return List.of();
    }

    return monitoreo("?donaciones=" + donacionIds.stream()
        .map(String::valueOf)
        .collect(Collectors.joining(",")));
  }

  @Override
  public EntregaVista entrega(long entregaId) {
    return http.obtener("/entregas/" + entregaId, new TypeReference<EntregaVista>() {});
  }

  @Override
  public List<String> subirFotos(long entregaId, List<ArchivoSubido> fotos) {
    return http.publicarArchivos(
        "/entregas/" + entregaId + "/fotos",
        "fotos",
        fotos,
        new TypeReference<FotosSubidas>() {}
    ).urlsOVacio();
  }

  @Override
  public void confirmarRecepcion(long entregaId, List<String> fotos) {
    http.publicar(
        "/entregas/" + entregaId + "/confirmacion",
        new ConfirmarRecepcion(fotos)
    );
  }

  private List<MonitoreoRutaVista> monitoreo(String filtro) {
    return http.obtener(
        "/monitoreo/rutas" + filtro,
        new TypeReference<List<MonitoreoRutaVista>>() {}
    );
  }

  private String rutaDeCamion(String patente) {
    return "/camiones/"
        + URLEncoder.encode(patente, StandardCharsets.UTF_8).replace("+", "%20");
  }
}
