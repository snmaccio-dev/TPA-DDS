package donatrack.web.integracion;

import donatrack.web.vista.EntidadVista;
import donatrack.web.vista.NecesidadVista;
import donatrack.web.vista.PerfilVista;
import donatrack.web.vista.SubcategoriaVista;

import java.util.List;

public interface ClienteDonaciones {

  PerfilVista perfil(String autorizacion);

  // Las dos altas son publicas: no llevan token porque quien se registra todavia no
  // tiene cuenta. Del otro lado, /registro/* esta declarado con RolDeAcceso.PUBLICO.
  void registrarDonante(RegistroDonante datos);

  void registrarBeneficiaria(RegistroBeneficiaria datos);

  List<EntidadVista> entidades(String autorizacion);

  // Donaciones ya acota el listado: a una beneficiaria le devuelve solo las suyas.
  List<NecesidadVista> necesidades(String autorizacion);

  void crearNecesidad(String autorizacion, NuevaNecesidad datos);

  // El catalogo de subcategorias, para que los formularios ofrezcan opciones validas.
  List<SubcategoriaVista> subcategorias(String autorizacion);
}
