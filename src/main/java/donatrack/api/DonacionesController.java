package donatrack.api;

import donatrack.gestion.GestorDonaciones;
import donatrack.model.donacion.CambioEstado;
import donatrack.model.donacion.Donacion;

import java.util.List;

public class DonacionesController {

  private final GestorDonaciones gestor = new GestorDonaciones();

  // GET /donaciones
  public List<Donacion> todas() {
    return gestor.todas();
  }

  // GET /donaciones/{id}
  public Donacion buscar(long id) {
    return gestor.buscar(id);
  }

  // POST /donaciones
  public Donacion crear(Donacion donacion) {
    return gestor.crear(donacion);
  }

  // DELETE /donaciones/{id}
  public void eliminar(long id) {
    gestor.eliminar(id);
  }

  // PATCH /donaciones/{id}/estado
  public void asignar(long id) {
    gestor.asignar(id);
  }

  public void planificarRuta(long id) {
    gestor.planificarRuta(id);
  }

  public void iniciarTraslado(long id) {
    gestor.iniciarTraslado(id);
  }

  public void confirmarEntrega(long id) {
    gestor.confirmarEntrega(id);
  }

  public void fallarEntrega(long id, String justificacion) {
    gestor.fallarEntrega(id, justificacion);
  }

  public void vencer(long id) {
    gestor.vencer(id);
  }

  // GET /donaciones/{id}/historial
  public List<CambioEstado> historial(long id) {
    return gestor.historial(id);
  }
}
