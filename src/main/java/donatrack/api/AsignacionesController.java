package donatrack.api;

import donatrack.gestion.GestorAsignaciones;
import donatrack.model.donacion.Donacion;
import donatrack.model.persona.Administrador;
import donatrack.model.persona.Beneficiaria;
import donatrack.repositorio.RepositorioDonaciones;

import java.util.List;

public class AsignacionesController {

  private final GestorAsignaciones gestor = new GestorAsignaciones();
  private final RepositorioDonaciones repositorioDonaciones =
      RepositorioDonaciones.getInstance();

  // POST /donaciones/{id}/asignacion — ejecuta los algoritmos a demanda
  public List<Beneficiaria> ejecutarAsignacion(long donacionId) {
    return gestor.ejecutarAsignacion(buscarDonacion(donacionId));
  }

  // GET /donaciones/{id}/asignacion — devuelve el ranking
  public List<Beneficiaria> obtenerPropuesta(long donacionId) {
    return gestor.obtenerPropuesta(buscarDonacion(donacionId));
  }

  // POST /donaciones/{id}/destinatario — confirma la beneficiaria final
  public void confirmarDestinatario(long donacionId,
                                    Beneficiaria destinatario,
                                    Administrador administrador) {
    gestor.confirmarDestino(donacionId, destinatario, administrador);
  }

  private Donacion buscarDonacion(long donacionId) {
    return repositorioDonaciones.buscarPorId(donacionId)
        .orElseThrow(() -> new IllegalArgumentException(
            "No existe la donación con ID " + donacionId
        ));
  }
}
