package donatrack.web.integracion;

import donatrack.web.vista.PerfilVista;

public interface ClienteDonaciones {

  PerfilVista perfil(String autorizacion);

  // Las dos altas son publicas: no llevan token porque quien se registra todavia no
  // tiene cuenta. Del otro lado, /registro/* esta declarado con RolDeAcceso.PUBLICO.
  void registrarDonante(RegistroDonante datos);

  void registrarBeneficiaria(RegistroBeneficiaria datos);
}
