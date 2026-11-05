package donatrack.web.integracion;

import donatrack.web.vista.CamionVista;
import donatrack.web.vista.EntregaVista;
import donatrack.web.vista.MonitoreoRutaVista;

import java.util.List;

public interface ClienteLogistica {

  List<CamionVista> camiones();

  CamionVista camion(String patente);

  void crearCamion(NuevoCamion camion);

  void actualizarCamion(String patente, CapacidadesDeCamion capacidades);

  List<MonitoreoRutaVista> monitoreoDeBeneficiaria(long beneficiariaId);

  List<MonitoreoRutaVista> monitoreoDeDonaciones(List<Long> donacionIds);

  EntregaVista entrega(long entregaId);

  List<String> subirFotos(long entregaId, List<ArchivoSubido> fotos);

  void confirmarRecepcion(long entregaId, List<String> fotos);
}
