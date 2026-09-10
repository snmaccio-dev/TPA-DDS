package donatrack.donaciones.controller;

import donatrack.donaciones.service.GestorDonaciones;
import donatrack.donaciones.domain.donacion.CambioEstado;
import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.donacion.Comprobante;
import donatrack.donaciones.infrastructure.notificacion.NotificadorWhatsApp;

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

  public void confirmarRecepcion(long id, List<String> fotos) {
    gestor.confirmarRecepcion(id, fotos);
  }

  public void marcarEntregaFallida(long id, String motivo) {
    gestor.marcarEntregaFallida(id, motivo);
  }

  public void marcarEnDeposito(long id) {
    gestor.marcarEnDeposito(id);
  }

  public void vencer(long id) {
    gestor.vencer(id);
  }

  // GET /donaciones/{id}/comprobante
  public Comprobante comprobante(long id) {
    return gestor.buscar(id).generarComprobante();
  }

  // GET /donaciones/{id}/historial
  public List<CambioEstado> historial(long id) {
    return gestor.historial(id);
  }
}
