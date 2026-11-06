package donatrack.web.integracion;

import com.fasterxml.jackson.core.type.TypeReference;
import donatrack.web.vista.EntidadVista;
import donatrack.web.vista.NecesidadVista;
import donatrack.web.vista.PerfilVista;

import java.util.List;

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
}
