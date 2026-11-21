package donatrack.web.integracion;

import donatrack.web.vista.DonacionVista;
import donatrack.web.vista.DonanteVista;
import donatrack.web.vista.EntidadVista;
import donatrack.web.vista.ImportacionVista;
import donatrack.web.vista.NecesidadVista;
import donatrack.web.vista.NotificacionVista;
import donatrack.web.vista.PerfilVista;
import donatrack.web.vista.PaginaVista;
import donatrack.web.vista.PropuestaVista;
import donatrack.web.vista.SubcategoriaVista;
import donatrack.web.vista.UbicacionEntidadVista;

import java.util.Collection;
import java.util.List;

public interface ClienteDonaciones {

  PerfilVista perfil(String autorizacion);

  // Las dos altas son publicas: no llevan token porque quien se registra todavia no
  // tiene cuenta. Del otro lado, /registro/* esta declarado con RolDeAcceso.PUBLICO.
  void registrarDonante(RegistroDonante datos);

  void registrarBeneficiaria(RegistroBeneficiaria datos);

  List<EntidadVista> entidades(String autorizacion);

  // Publico, sin token: id, razon social y coordenadas de las entidades pedidas que ya
  // estan ubicadas (las demas no vienen). Una sola consulta para todos los ids.
  List<UbicacionEntidadVista> ubicacionesDeEntidades(Collection<Long> ids);

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

  // GET /donantes?pagina=&busqueda=. Solo admin. Paginado: pagina empieza en 1 y
  // busqueda (opcional) filtra por nombre o razon social, documento o mail.
  PaginaVista<DonanteVista> donantes(String autorizacion, String busqueda, int pagina);

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

  void marcarNotificacionLeida(String autorizacion, long notificacionId);

  void deshacerVencimiento(String autorizacion, long donacionId);

  // POST /donaciones/{id}/confirmar-recepcion. Solo admin.
  //void confirmarRecepcion(String autorizacion, long donacionId);

    void confirmarRecepcion(
      String autorizacion,
      long donacionId,
      String donanteDocumento
  );

  // POST /donantes/importaciones (multipart, campo "archivo"). Asincronico: devuelve el
  // id y el procesamiento sigue en Donaciones. Solo admin.
  long importarDonantes(String autorizacion, ArchivoSubido archivo);

  // GET /donantes/importaciones: las ultimas, de la mas reciente a la mas vieja.
  List<ImportacionVista> importaciones(String autorizacion);

  // GET /donantes/importaciones/{id}.
  ImportacionVista importacion(String autorizacion, long importacionId);
}
