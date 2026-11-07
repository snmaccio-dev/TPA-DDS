package donatrack.web.integracion;

import com.fasterxml.jackson.core.type.TypeReference;
import donatrack.web.vista.DonacionVista;
import donatrack.web.vista.DonanteVista;
import donatrack.web.vista.EntidadVista;
import donatrack.web.vista.NecesidadVista;
import donatrack.web.vista.NotificacionVista;
import donatrack.web.vista.PerfilVista;
import donatrack.web.vista.PropuestaVista;
import donatrack.web.vista.SubcategoriaVista;

import java.util.List;
import java.util.Map;

public class ClienteDonacionesHttp implements ClienteDonaciones {

  private static final String SERVICIO = "Donaciones";
  private static final String SIN_TOKEN = null;

  private final ClienteHttp http;

  public ClienteDonacionesHttp(String urlBase) {
    this.http = new ClienteHttp(urlBase, SERVICIO);
  }

  @Override
  public PerfilVista perfil(String autorizacion) {
    return http.obtener(autorizacion, "/usuarios/yo", new TypeReference<PerfilVista>() {});
  }

  @Override
  public void registrarDonante(RegistroDonante datos) {
    http.publicar(SIN_TOKEN, "/registro/donantes", datos);
  }

  @Override
  public void registrarBeneficiaria(RegistroBeneficiaria datos) {
    http.publicar(SIN_TOKEN, "/registro/beneficiarias", datos);
  }

  @Override
  public List<EntidadVista> entidades(String autorizacion) {
    return http.obtener(autorizacion, "/entidades",
        new TypeReference<List<EntidadVista>>() {});
  }

  @Override
  public List<NecesidadVista> necesidades(String autorizacion) {
    return http.obtener(autorizacion, "/necesidades",
        new TypeReference<List<NecesidadVista>>() {});
  }

  @Override
  public void crearNecesidad(String autorizacion, NuevaNecesidad datos) {
    http.publicar(autorizacion, "/necesidades", datos);
  }

  @Override
  public List<SubcategoriaVista> subcategorias(String autorizacion) {
    return http.obtener(autorizacion, "/subcategorias",
        new TypeReference<List<SubcategoriaVista>>() {});
  }

  @Override
  public List<DonacionVista> donaciones(String autorizacion, FiltroDonacionesWeb filtro) {
    String ruta = "/donaciones" + (filtro == null ? "" : filtro.aQueryString());
    return http.obtener(autorizacion, ruta, new TypeReference<List<DonacionVista>>() {});
  }

  @Override
  public void marcarDonacionVencida(String autorizacion, long donacionId) {
    http.publicar(autorizacion, "/donaciones/" + donacionId + "/vencer", Map.of());
  }

  @Override
  public void crearDonacion(String autorizacion, NuevaDonacion datos) {
    http.publicar(autorizacion, "/donaciones", datos);
  }

  @Override
  public List<DonanteVista> donantes(String autorizacion) {
    return http.obtener(autorizacion, "/donantes",
        new TypeReference<List<DonanteVista>>() {});
  }

  @Override
  public void registrarDonanteAdmin(String autorizacion, NuevoDonanteAdmin datos) {
    http.publicar(autorizacion, "/donantes", datos);
  }

  @Override
  public List<PropuestaVista> propuestasDeAsignacion(String autorizacion) {
    return http.obtener(autorizacion, "/asignaciones/propuestas",
        new TypeReference<List<PropuestaVista>>() {});
  }

  @Override
  public ResultadoEjecucion ejecutarAsignaciones(String autorizacion) {
    return http.consultar(autorizacion, "/asignaciones/ejecutar", Map.of(),
        new TypeReference<ResultadoEjecucion>() {});
  }

  @Override
  public void confirmarDestinatario(String autorizacion, long donacionId, long beneficiariaId) {
    http.publicar(autorizacion, "/donaciones/" + donacionId + "/destinatario",
        Map.of("beneficiariaId", beneficiariaId));
  }

  @Override
  public List<NotificacionVista> misNotificaciones(String autorizacion) {
    return http.obtener(autorizacion, "/notificaciones/mias",
        new TypeReference<List<NotificacionVista>>() {});
  }

  @Override
  public void marcarTodasLasNotificacionesLeidas(String autorizacion) {
    http.publicar(autorizacion, "/notificaciones/mias/leidas", Map.of());
  }
}
