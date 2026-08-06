package donatrack.gestion;

import donatrack.model.donacion.Donacion;
import donatrack.model.entidad.EntidadBeneficiaria;
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
      EntidadBeneficiaria entidad,
      Donacion donacion) {

    enviar(
        entidad,
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

    involucrados.stream()
        .map(persona -> {
          enviar(
              persona,
              "La ruta de entrega comenzó. "
                  + "Podés seguirla aquí: " + linkMapa
          );
          return persona;
        })
        .toList();
  }

  public void notificarEntregaExitosa(
      Persona donante,
      EntidadBeneficiaria entidad,
      String comprobante) {

    enviar(
        donante,
        "La entrega fue realizada correctamente. "
            + comprobante
    );

    enviar(
        entidad,
        "Confirmaste la recepción de la donación. "
            + comprobante
    );
  }

  public void notificarEntregaFallida(
      Persona donante,
      EntidadBeneficiaria entidad,
      List<Persona> administradores,
      String motivo) {

    enviar(
        donante,
        "La entrega no pudo completarse: " + motivo
    );

    enviar(
        entidad,
        "La entrega no pudo completarse: " + motivo
    );

    administradores.stream()
        .map(admin -> {
          enviar(
              admin,
              "Entrega fallida: " + motivo
          );
          return admin;
        })
        .toList();
  }


  private void enviar(Persona destinatario, String mensaje) {
    notificadores.stream()
        .map(notificador -> {
          notificador.notificar(
              destinatario.getUsuario().getNombre(),
              mensaje
          );
          return notificador;
        })
        .toList();
  }
}
