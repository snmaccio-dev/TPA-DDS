package donatrack.donaciones.controller;

import donatrack.donaciones.service.GestorAsignaciones;
import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.donacion.asignacion.PropuestaAsignacion;
import donatrack.donaciones.domain.persona.Administrador;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.repository.RepositorioDonaciones;

import java.util.List;

public class AsignacionesController {

  private final GestorAsignaciones gestor;
  private final RepositorioDonaciones repositorioDonaciones =
      RepositorioDonaciones.getInstance();

  public AsignacionesController(GestorAsignaciones gestor) {
    this.gestor = gestor;
  }

  // POST /donaciones/{id}/asignacion — ejecuta los algoritmos a demanda
  public List<Beneficiaria> ejecutarAsignacion(long donacionId) {
    return gestor.ejecutarAsignacion(buscarDonacion(donacionId));
  }

  // GET /donaciones/{id}/asignacion — devuelve el ranking
  public List<Beneficiaria> obtenerPropuesta(long donacionId) {
    return gestor.obtenerPropuesta(buscarDonacion(donacionId));
  }

  // GET /asignaciones/propuestas — listado que ve la administradora
  public List<PropuestaAsignacion> propuestas() {
    return gestor.propuestas();
  }

  // GET /donaciones/{id}/propuesta — propuesta persistida de una donación puntual
  public PropuestaAsignacion propuesta(long donacionId) {
    return gestor.buscarPropuesta(donacionId)
        .orElseThrow(() -> new IllegalArgumentException(
            "No hay propuesta generada para la donación " + donacionId
        ));
  }

  // POST /asignaciones/ejecutar — dispara el matchmaking programado a mano
  public int ejecutarMatchmakingProgramado() {
    return gestor.ejecutarMatchmakingProgramado();
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