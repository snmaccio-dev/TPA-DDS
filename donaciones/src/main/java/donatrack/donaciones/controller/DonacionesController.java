package donatrack.donaciones.controller;

import donatrack.donaciones.service.GestorDonaciones;
import donatrack.donaciones.service.GestorNotificaciones;
import donatrack.donaciones.domain.donacion.Bien;
import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.donacion.Comprobante;
import donatrack.donaciones.domain.donacion.estado.EstadoDonacion;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.infrastructure.notificacion.NotificadorEmail;
import donatrack.donaciones.infrastructure.notificacion.NotificadorSMS;
import donatrack.donaciones.infrastructure.notificacion.NotificadorWhatsApp;

import java.util.List;

public class DonacionesController {

  private final GestorDonaciones gestor =
      new GestorDonaciones(new GestorNotificaciones(List.of(
          new NotificadorEmail(),
          new NotificadorSMS(),
          new NotificadorWhatsApp()
      )));

  // GET /donaciones
  public List<Donacion> todas() {
    return gestor.todas();
  }

  // GET /donaciones/{id}
  public Donacion buscar(long id) {
    return gestor.buscar(id);
  }

  // POST /donaciones
  public List<Donacion> crear(List<Bien> bienes, Donante donante, String descripcion) {
    return gestor.crear(bienes, donante, descripcion);
  }

  // DELETE /donaciones/{id}
  public void eliminar(long id) {
    gestor.eliminar(id);
  }

  public void confirmarDestino(long id, Beneficiaria destinatario) {
    gestor.confirmarDestino(id, destinatario);
  }

  public void marcarListaParaEntregar(long id) {
    gestor.marcarListaParaEntregar(id);
  }

  public void marcarEnTraslado(long id) {
    gestor.marcarEnTraslado(id);
  }

  public void marcarEntregada(long id, java.time.LocalDateTime fechaHora, String patenteCamion) {
    gestor.marcarEntregada(id, fechaHora, patenteCamion);
  }

  public void marcarEntregaFallida(long id, String motivo) {
    gestor.marcarEntregaFallida(id, motivo);
  }

  public void marcarEnDeposito(long id) {
    gestor.marcarEnDeposito(id);
  }

  public void marcarVencida(long id) {
    gestor.marcarVencida(id);
  }

  // GET /donaciones/{id}/comprobante
  public Comprobante comprobante(long id) {
    return gestor.buscar(id).generarComprobante();
  }

  // GET /donaciones/{id}/historial
  public List<EstadoDonacion> historial(long id) {
    return gestor.historial(id);
  }
}