package donatrack.donaciones.service;

import donatrack.donaciones.domain.DatosInvalidosException;
import donatrack.donaciones.domain.contacto.MedioContacto;
import donatrack.donaciones.domain.contacto.TipoContacto;
import donatrack.donaciones.domain.donacion.Comprobante;
import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.notificacion.Notificacion;
import donatrack.donaciones.domain.notificacion.Notificador;
import donatrack.donaciones.domain.notificacion.TipoNotificacion;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.domain.persona.Persona;
import donatrack.donaciones.repository.RepositorioNotificaciones;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

// Todas las notificaciones se persisten (incluso las fallidas y las de solo
// plataforma) para que el web muestre a cada usuario su lista y para auditoria.
// Las fallas de envio no se propagan: no pueden deshacer el cambio de estado
// que las dispara.
public class GestorNotificaciones {

  private final Map<TipoContacto, Notificador> porTipo;
  private final RepositorioNotificaciones repositorio;

  public GestorNotificaciones(List<Notificador> notificadores,
                              RepositorioNotificaciones repositorio) {
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
    this.repositorio = repositorio;
  }

  // Entry point principal para un donante. Nunca propaga fallas del canal
  // externo: siempre guarda una Notificacion en estado ENVIADA o FALLIDA y
  // la devuelve. Solo valida datos obligatorios con DatosInvalidosException.
  public Notificacion notificar(Donante donante,
                                TipoNotificacion tipo,
                                String mensaje,
                                Donacion donacion) {
    if (donante == null) {
      throw new DatosInvalidosException("El donante destinatario es obligatorio.");
    }
    return enviarYPersistir(
        donante.getPersona(),
        () -> Notificacion.paraDonante(donante, tipo, mensaje, donacion));
  }

  public Notificacion notificar(Beneficiaria beneficiaria,
                                TipoNotificacion tipo,
                                String mensaje,
                                Donacion donacion) {
    if (beneficiaria == null) {
      throw new DatosInvalidosException("La beneficiaria destinataria es obligatoria.");
    }
    return enviarYPersistir(
        beneficiaria.getPersona(),
        () -> Notificacion.paraBeneficiaria(beneficiaria, tipo, mensaje, donacion));
  }

  // Notificacion solo de plataforma: queda guardada con estadoEnvio NO_APLICA,
  // sin intento de envio externo. Los admins la ven cuando entran.
  // TODO: envio externo a admins. Para mandar mail a todos los admins habria
  // que listarlos contra Keycloak (GET /admin/realms/{realm}/users con el
  // rol ADMINISTRADOR) y despacharlos uno por uno. Fuera de alcance por ahora.
  public Notificacion notificarAdministradores(TipoNotificacion tipo,
                                               String mensaje,
                                               Donacion donacion) {
    Notificacion notificacion = Notificacion.paraAdministradores(tipo, mensaje, donacion);
    repositorio.guardar(notificacion);
    return notificacion;
  }

  private Notificacion enviarYPersistir(Persona personaDeContacto,
                                        Supplier<Notificacion> fabrica) {
    Notificacion notificacion = fabrica.get();
    intentarEnviar(notificacion, personaDeContacto);
    repositorio.guardar(notificacion);
    return notificacion;
  }

  private void intentarEnviar(Notificacion notificacion, Persona personaDeContacto) {
    Optional<MedioContacto> medioOpt = personaDeContacto.getContactoParaNotificar();
    if (medioOpt.isEmpty()) {
      notificacion.marcarFallida("La persona no tiene ningun medio de contacto cargado.");
      return;
    }
    MedioContacto medio = medioOpt.get();
    Notificador notificador = porTipo.get(medio.getTipo());
    if (notificador == null) {
      notificacion.marcarFallida("No hay notificador registrado para " + medio.getTipo() + ".");
      return;
    }
    try {
      notificador.notificar(medio.getValor(), notificacion.getMensaje());
      notificacion.marcarEnviada(medio.getTipo(), medio.getValor());
    } catch (RuntimeException e) {
      notificacion.marcarFallida(
          "El notificador " + medio.getTipo() + " fallo: " + e.getMessage());
    }
  }

  // === Metodos especificos armados sobre notificar/notificarAdministradores ===

  public Notificacion notificarAusenciaPlataforma(Donante donante) {
    return notificar(
        donante,
        TipoNotificacion.AUSENCIA_PLATAFORMA,
        "Hace mas de 20 dias que no interactuas con la plataforma. "
            + "Realiza una nueva donacion!",
        null
    );
  }

  public Notificacion notificarDonacionAsignadaBeneficiario(Beneficiaria beneficiaria,
                                                            Donacion donacion) {
    return notificar(
        beneficiaria,
        TipoNotificacion.DONACION_ASIGNADA,
        "Se te asigno una nueva donacion de ["
            + donacion.getSubcategoria().getNombre() + "] segun tus necesidades.",
        donacion
    );
  }

  public Notificacion notificarDonacionAsignadaDonante(Donante donante, Donacion donacion) {
    return notificar(
        donante,
        TipoNotificacion.DONACION_ASIGNADA,
        "Tu donacion de [" + donacion.getSubcategoria().getNombre()
            + "] fue asignada a una entidad beneficiaria.",
        donacion
    );
  }

  public Notificacion notificarInicioRuta(Donante donante, Donacion donacion, String linkMapa) {
    return notificar(
        donante,
        TipoNotificacion.INICIO_RUTA,
        "La ruta de entrega de [" + donacion.getSubcategoria().getNombre()
            + "] comenzo. Podes seguirla aqui: " + linkMapa,
        donacion
    );
  }

  public Notificacion notificarInicioRuta(Beneficiaria beneficiaria, Donacion donacion, String linkMapa) {
    return notificar(
        beneficiaria,
        TipoNotificacion.INICIO_RUTA,
        "La ruta de entrega de [" + donacion.getSubcategoria().getNombre()
            + "] comenzo. Podes seguirla aqui: " + linkMapa,
        donacion
    );
  }

  public void notificarEntregaExitosa(Donante donante,
                                      Beneficiaria beneficiaria,
                                      Donacion donacion,
                                      Comprobante comprobante) {
    String detalle = "Comprobante #" + comprobante.donacionId()
        + " | Fecha: " + comprobante.fecha()
        + " | Camion: " + comprobante.patenteCamion();

    notificar(
        donante,
        TipoNotificacion.ENTREGA_EXITOSA,
        "La entrega fue realizada correctamente. " + detalle,
        donacion
    );
    notificar(
        beneficiaria,
        TipoNotificacion.ENTREGA_EXITOSA,
        "Confirmaste la recepcion de la donacion. " + detalle,
        donacion
    );
  }

  public void notificarEntregaFallida(Donante donante,
                                      Beneficiaria beneficiaria,
                                      Donacion donacion,
                                      String motivo) {
    String motivoSeguro = motivo == null || motivo.isBlank()
        ? "(sin motivo indicado)" : motivo;
    notificar(
        donante,
        TipoNotificacion.ENTREGA_FALLIDA,
        "La entrega de [" + donacion.getSubcategoria().getNombre()
            + "] no pudo completarse: " + motivoSeguro,
        donacion
    );
    notificar(
        beneficiaria,
        TipoNotificacion.ENTREGA_FALLIDA,
        "La entrega de [" + donacion.getSubcategoria().getNombre()
            + "] no pudo completarse: " + motivoSeguro,
        donacion
    );
    notificarAdministradores(
        TipoNotificacion.ENTREGA_FALLIDA,
        "Entrega fallida de la donacion #" + donacion.getId() + ": " + motivoSeguro,
        donacion
    );
  }

  // Mensaje generico de cambio de estado para los estados que no tienen uno
  // propio (LISTA_PARA_ENTREGAR, EN_DEPOSITO, VENCIDA).
  public Notificacion notificarCambioDeEstado(Donante donante,
                                              Donacion donacion,
                                              String estadoAnterior,
                                              String estadoNuevo) {
    return notificar(
        donante,
        TipoNotificacion.CAMBIO_ESTADO,
        "Su donacion de [" + donacion.getSubcategoria().getNombre()
            + "] cambio de estado: " + estadoAnterior + " → " + estadoNuevo,
        donacion
    );
  }
}
