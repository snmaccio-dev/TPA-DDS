package donatrack.web.integracion;

import com.fasterxml.jackson.core.type.TypeReference;
import donatrack.web.vista.PerfilVista;

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
}
