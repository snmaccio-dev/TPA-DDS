package donatrack.web.integracion;

import donatrack.web.vista.CamionVista;
import donatrack.web.vista.EntregaPublicaVista;
import donatrack.web.vista.EntregaVista;
import donatrack.web.vista.MonitoreoRutaVista;
import donatrack.web.vista.PlanificacionVista;
import donatrack.web.vista.RutaVista;

import java.util.List;

public interface ClienteLogistica {

  // Sin autorizacion: el endpoint esta declarado PUBLICO en Logistica, porque alimenta
  // la pantalla de transparencia que se ve sin iniciar sesion.
  List<EntregaPublicaVista> entregasPublicas();

  List<CamionVista> camiones(String autorizacion);

  CamionVista camion(String autorizacion, String patente);

  void crearCamion(String autorizacion, NuevoCamion camion);

  void actualizarCamion(String autorizacion, String patente, CapacidadesDeCamion capacidades);

  List<MonitoreoRutaVista> monitoreoDeRutasEnCurso(String autorizacion);

  PlanificacionVista planificacion(String autorizacion);

  List<RutaVista> rutasDelChofer(String autorizacion);

  void iniciarRuta(String autorizacion, long rutaId);

  EntregaVista entrega(String autorizacion, long entregaId);

  List<String> subirFotos(String autorizacion, long entregaId, List<ArchivoSubido> fotos);

  void confirmarRecepcion(String autorizacion, long entregaId, List<String> fotos);
}
