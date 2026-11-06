package donatrack.web.integracion;

import donatrack.web.vista.PerfilVista;

public interface ClienteDonaciones {

  PerfilVista perfil(String autorizacion);
}
