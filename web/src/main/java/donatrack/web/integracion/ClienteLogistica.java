package donatrack.web.integracion;

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

  List<MonitoreoRutaVista> monitoreoDeRutasEnCurso(String autorizacion);

  PlanificacionVista planificacion(String autorizacion);

  List<RutaVista> rutasDelChofer(String autorizacion);

  void iniciarRuta(String autorizacion, long rutaId);

  EntregaVista entrega(String autorizacion, long entregaId);

  // Entregas de la beneficiaria autenticada que todavia puede confirmar. Logistica
  // deduce la beneficiaria a partir del token.
  List<EntregaVista> entregasPendientesDeConfirmar(String autorizacion);

  List<String> subirFotos(String autorizacion, long entregaId, List<ArchivoSubido> fotos);

  void confirmarRecepcion(String autorizacion, long entregaId, List<String> fotos);
}
