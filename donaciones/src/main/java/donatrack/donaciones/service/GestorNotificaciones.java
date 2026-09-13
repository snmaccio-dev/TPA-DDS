package donatrack.donaciones.service;

import donatrack.donaciones.domain.contacto.MedioContacto;
import donatrack.donaciones.domain.contacto.TipoContacto;
import donatrack.donaciones.domain.donacion.Comprobante;
import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.notificacion.Notificador;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.Persona;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class GestorNotificaciones {

  private final Map<TipoContacto, Notificador> porTipo;

  public GestorNotificaciones(List<Notificador> notificadores) {
    Map<TipoContacto, Notificador> mapa = new EnumMap<>(TipoContacto.class);
    for (Notificador n : notificadores) {
      Notificador previo = mapa.put(n.tipoAtendido(), n);
      if (previo != null) {
        throw new IllegalArgumentException(
            "Hay mas de un notificador registrado para el tipo " + n.tipoAtendido()
        );
      }
    }
    this.porTipo = mapa;
  }

  public void notificar(Persona destinatario, String mensaje) {
    MedioContacto medio = destinatario.getContactoParaNotificar()
        .orElseThrow(() -> new IllegalStateException(
            "La persona no tiene ningun medio de contacto cargado"));
    Notificador notificador = porTipo.get(medio.getTipo());
    if (notificador == null) {
      throw new IllegalStateException(
          "No hay notificador registrado para " + medio.getTipo());
    }
    notificador.notificar(medio.getValor(), mensaje);
  }

  public void notificarAusenciaPlataforma(Persona donante) {
    notificar(
        donante,
        "Hace más de 20 días que no interactúas con la plataforma. "
            + "¡Realizá una nueva donación!"
    );
  }

  public void notificarDonacionAsignadaBeneficiario(
      Beneficiaria beneficiaria,
      Donacion donacion) {

    notificar(
        beneficiaria.getPersona(),
        "Se te asignó una nueva donación según tus necesidades."
    );
  }

  public void notificarDonacionAsignadaDonante(
      Persona donante,
      Donacion donacion) {

    notificar(
        donante,
        "Tu donación fue asignada a una entidad beneficiaria."
    );
  }

  public void notificarInicioRuta(
      List<Persona> involucrados,
      String linkMapa) {

    involucrados.forEach(persona ->
        notificar(
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

    notificar(
        donante,
        "La entrega fue realizada correctamente. " + detalle
    );

    notificar(
        beneficiaria.getPersona(),
        "Confirmaste la recepción de la donación. " + detalle
    );
  }

  public void notificarEntregaFallida(
      Persona donante,
      Beneficiaria beneficiaria,
      List<Persona> administradores,
      String motivo) {

    notificar(
        donante,
        "La entrega no pudo completarse: " + motivo
    );

    notificar(
        beneficiaria.getPersona(),
        "La entrega no pudo completarse: " + motivo
    );

    administradores.forEach(admin ->
        notificar(admin, "Entrega fallida: " + motivo)
    );
  }
}