package donatrack.gestion;

import donatrack.model.donacion.Donacion;
import donatrack.model.logistica.Comprobante;
import donatrack.model.persona.Beneficiaria;
import donatrack.model.persona.Persona;
import donatrack.notificacion.Notificador;

import java.util.List;

public class GestorNotificaciones {

  private final List<Notificador> notificadores;

  public GestorNotificaciones(List<Notificador> notificadores) {
    this.notificadores = notificadores;
  }

  public void notificarAusenciaPlataforma(Persona donante) {
    enviar(
        donante,
        "Hace más de 20 días que no interactúas con la plataforma. "
            + "¡Realizá una nueva donación!"
    );
  }

  public void notificarDonacionAsignadaBeneficiario(
      Beneficiaria beneficiaria,
      Donacion donacion) {

    enviar(
        beneficiaria.getPersona(),
        "Se te asignó una nueva donación según tus necesidades."
    );
  }

  public void notificarDonacionAsignadaDonante(
      Persona donante,
      Donacion donacion) {

    enviar(
        donante,
        "Tu donación fue asignada a una entidad beneficiaria."
    );
  }

  public void notificarInicioRuta(
      List<Persona> involucrados,
      String linkMapa) {

    involucrados.forEach(persona ->
        enviar(
            persona,
            "La ruta de entrega comenzó. "
                + "Podés seguirla aquí: " + linkMapa
        )
    );
  }

  public void notificarEntregaExitosa(
      Persona donante,
      Beneficiaria beneficiaria,
      Comprobante comprobante) {

    String detalle = "Comprobante #" + comprobante.donacionId()
        + " | Fecha: " + comprobante.fecha()
        + " | Camión: " + comprobante.patenteCamion();

    enviar(
        donante,
        "La entrega fue realizada correctamente. " + detalle
    );

    enviar(
        beneficiaria.getPersona(),
        "Confirmaste la recepción de la donación. " + detalle
    );
  }

  public void notificarEntregaFallida(
      Persona donante,
      Beneficiaria beneficiaria,
      List<Persona> administradores,
      String motivo) {

    enviar(
        donante,
        "La entrega no pudo completarse: " + motivo
    );

    enviar(
        beneficiaria.getPersona(),
        "La entrega no pudo completarse: " + motivo
    );

    administradores.forEach(admin ->
        enviar(admin, "Entrega fallida: " + motivo)
    );
  }


  private void enviar(Persona destinatario, String mensaje) {
    notificadores.forEach(notificador ->
        notificador.notificar(
            destinatario.getUsuario().getNombre(),
            mensaje
        )
    );
  }
}
