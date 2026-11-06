package donatrack.web.integracion;

import donatrack.web.vista.CamionVista;
import donatrack.web.vista.EntregaVista;
import donatrack.web.vista.MonitoreoRutaVista;

import java.util.List;

public interface ClienteLogistica {

  List<CamionVista> camiones(String autorizacion);

  CamionVista camion(String autorizacion, String patente);

  void crearCamion(String autorizacion, NuevoCamion camion);

  void actualizarCamion(String autorizacion, String patente, CapacidadesDeCamion capacidades);

  List<MonitoreoRutaVista> monitoreoDeRutasEnCurso(String autorizacion);

  EntregaVista entrega(String autorizacion, long entregaId);

  List<String> subirFotos(String autorizacion, long entregaId, List<ArchivoSubido> fotos);

  void confirmarRecepcion(String autorizacion, long entregaId, List<String> fotos);
}
