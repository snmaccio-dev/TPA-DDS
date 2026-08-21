package donatrack.api;

import donatrack.gestion.GestorDonaciones;
import donatrack.model.donacion.CambioEstado;
import donatrack.model.donacion.Donacion;
import donatrack.notificacion.NotificadorWhatsApp;

import java.util.List;

public class DonacionesController {

  private final GestorDonaciones gestor =
      new GestorDonaciones(new NotificadorWhatsApp());

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

  public void marcarEnReparto(long id) {
    gestor.marcarEnReparto(id);
  }

  public void devolverAlDeposito(long id) {
    gestor.devolverAlDeposito(id);
  }

  public void vencer(long id) {
    gestor.vencer(id);
  }

  // GET /donaciones/{id}/historial
  public List<CambioEstado> historial(long id) {
    return gestor.historial(id);
  }
}
