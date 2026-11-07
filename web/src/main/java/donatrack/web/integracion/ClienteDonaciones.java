package donatrack.web.integracion;

import donatrack.web.vista.DonacionVista;
import donatrack.web.vista.DonanteVista;
import donatrack.web.vista.EntidadVista;
import donatrack.web.vista.NecesidadVista;
import donatrack.web.vista.NotificacionVista;
import donatrack.web.vista.PerfilVista;
import donatrack.web.vista.PropuestaVista;
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

  // GET /donaciones con filtros opcionales. La API ya recorta la lista por rol:
  // al donante le devuelve las suyas, a la beneficiaria las asignadas a ella,
  // al admin todas.
  List<DonacionVista> donaciones(String autorizacion, FiltroDonacionesWeb filtro);

  // POST /donaciones/{id}/vencer. Solo admin.
  void marcarDonacionVencida(String autorizacion, long donacionId);

  // POST /donaciones. Solo admin, con un bien por formulario.
  void crearDonacion(String autorizacion, NuevaDonacion datos);

  // GET /donantes. Solo admin.
  List<DonanteVista> donantes(String autorizacion);

  // POST /donantes. Alta desde el panel de administracion.
  void registrarDonanteAdmin(String autorizacion, NuevoDonanteAdmin datos);

  // GET /asignaciones/propuestas. Solo admin.
  List<PropuestaVista> propuestasDeAsignacion(String autorizacion);

  // POST /asignaciones/ejecutar. Devuelve el resultado para mostrar la advertencia
  // "Se asignaron X donaciones; Y no pudieron procesarse." cuando corresponde.
  ResultadoEjecucion ejecutarAsignaciones(String autorizacion);

  // POST /donaciones/{id}/destinatario. Confirma la beneficiaria final de una donacion.
  void confirmarDestinatario(String autorizacion, long donacionId, long beneficiariaId);

  // GET /notificaciones/mias.
  List<NotificacionVista> misNotificaciones(String autorizacion);

  // POST /notificaciones/mias/leidas. Marca todas como leidas en una sola llamada:
  // ClienteHttp no tiene PATCH, asi que no hay marcado individual.
  void marcarTodasLasNotificacionesLeidas(String autorizacion);
}
