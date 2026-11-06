package donatrack.donaciones.domain.notificacion;

import donatrack.donaciones.domain.catalogo.Categoria;
import donatrack.donaciones.domain.catalogo.Subcategoria;
import donatrack.donaciones.domain.catalogo.Unidades;
import donatrack.donaciones.domain.contacto.MedioContacto;
import donatrack.donaciones.domain.contacto.TipoContacto;
import donatrack.donaciones.domain.donacion.Bien;
import donatrack.donaciones.domain.donacion.CondicionBien;
import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.domain.persona.Genero;
import donatrack.donaciones.domain.persona.PersonaHumana;
import donatrack.donaciones.domain.persona.PersonaJuridica;
import donatrack.donaciones.domain.persona.TipoOrganizacion;
import donatrack.donaciones.service.GestorNotificaciones;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class NotificacionesDeDonacionObserverTest {

  private static final String LINK_DONANTE = "http://web/seguimiento-donante";
  private static final String LINK_BENEF = "http://web/seguimiento-beneficiario";

  @Test
  public void asignacionRealizadaNotificaADonanteYBeneficiaria() {
    GestorNotificaciones gestor = mock(GestorNotificaciones.class);
    Donante donante = donante();
    Beneficiaria beneficiaria = beneficiaria();
    Donacion donacion = donacionAsignadaA(donante, beneficiaria);
    NotificacionesDeDonacionObserver observer =
        new NotificacionesDeDonacionObserver(gestor, LINK_DONANTE, LINK_BENEF);

    observer.onCambioEstado(donacion, "EN_DEPOSITO", "ASIGNACION_REALIZADA");

    verify(gestor).notificarDonacionAsignadaDonante(eq(donante), eq(donacion));
    verify(gestor).notificarDonacionAsignadaBeneficiario(eq(beneficiaria), eq(donacion));
  }

  @Test
  public void enTrasladoAvisaAlDonanteYBeneficiariaConSusLinks() {
    GestorNotificaciones gestor = mock(GestorNotificaciones.class);
    Donante donante = donante();
    Beneficiaria beneficiaria = beneficiaria();
    Donacion donacion = donacionAsignadaA(donante, beneficiaria);
    NotificacionesDeDonacionObserver observer =
        new NotificacionesDeDonacionObserver(gestor, LINK_DONANTE, LINK_BENEF);

    observer.onCambioEstado(donacion, "LISTA_PARA_ENTREGAR", "EN_TRASLADO");

    verify(gestor).notificarInicioRuta(eq(donante), eq(donacion), eq(LINK_DONANTE));
    verify(gestor).notificarInicioRuta(eq(beneficiaria), eq(donacion), eq(LINK_BENEF));
  }

  @Test
  public void entregadaDisparaEntregaExitosaConComprobante() throws Exception {
    GestorNotificaciones gestor = mock(GestorNotificaciones.class);
    Donante donante = donante();
    Beneficiaria beneficiaria = beneficiaria();
    Donacion donacion = donacionAsignadaA(donante, beneficiaria);
    // generarComprobante() exige id no null. Como no estamos en una DB, lo
    // seteamos por reflexion.
    forzarId(donacion, 42L);
    donacion.marcarListaParaEntregar();
    donacion.marcarEnTraslado();
    donacion.marcarEntregada(LocalDateTime.of(2026, 3, 1, 10, 0), "AAA111");

    NotificacionesDeDonacionObserver observer =
        new NotificacionesDeDonacionObserver(gestor, LINK_DONANTE, LINK_BENEF);
    observer.onCambioEstado(donacion, "EN_TRASLADO", "ENTREGADA");

    verify(gestor).notificarEntregaExitosa(
        eq(donante), eq(beneficiaria), eq(donacion), any());
  }

  @Test
  public void entregaFallidaUsaLaJustificacionDelUltimoEstado() {
    GestorNotificaciones gestor = mock(GestorNotificaciones.class);
    Donante donante = donante();
    Beneficiaria beneficiaria = beneficiaria();
    Donacion donacion = donacionAsignadaA(donante, beneficiaria);
    donacion.marcarListaParaEntregar();
    donacion.marcarEnTraslado();
    donacion.marcarEntregaFallida("no habia nadie");

    NotificacionesDeDonacionObserver observer =
        new NotificacionesDeDonacionObserver(gestor, LINK_DONANTE, LINK_BENEF);
    observer.onCambioEstado(donacion, "EN_TRASLADO", "ENTREGA_FALLIDA");

    verify(gestor).notificarEntregaFallida(
        eq(donante), eq(beneficiaria), eq(donacion), eq("no habia nadie"));
  }

  @Test
  public void otrosCambiosMandanCambioDeEstadoAlDonante() {
    GestorNotificaciones gestor = mock(GestorNotificaciones.class);
    Donante donante = donante();
    Donacion donacion = new Donacion(List.of(unaCampera()), donante, "demo");
    NotificacionesDeDonacionObserver observer =
        new NotificacionesDeDonacionObserver(gestor, LINK_DONANTE, LINK_BENEF);

    observer.onCambioEstado(donacion, "ASIGNACION_REALIZADA", "LISTA_PARA_ENTREGAR");

    verify(gestor).notificarCambioDeEstado(
        eq(donante), eq(donacion),
        eq("ASIGNACION_REALIZADA"), eq("LISTA_PARA_ENTREGAR"));
    verify(gestor, never()).notificarDonacionAsignadaDonante(any(), any());
  }

  // === Helpers ===

  private Donante donante() {
    PersonaHumana ana = new PersonaHumana("Ana", "Perez", 30, "11111111", Genero.FEMENINO);
    ana.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, "ana@mail.com"));
    return new Donante(ana);
  }

  private Beneficiaria beneficiaria() {
    PersonaJuridica escuela = new PersonaJuridica(
        "30-11111111-1", "Escuela X", TipoOrganizacion.INSTITUCION, "Educacion");
    escuela.agregarMedioContacto(new MedioContacto(TipoContacto.EMAIL, "escuela@mail.com"));
    return new Beneficiaria(escuela);
  }

  private Bien unaCampera() {
    Subcategoria ropa = new Subcategoria(
        "Camperas", new Categoria("Vestimenta"), Unidades.UNIDADES);
    return new Bien("Campera", ropa, 1, CondicionBien.USADO);
  }

  private Donacion donacionAsignadaA(Donante donante, Beneficiaria beneficiaria) {
    Donacion donacion = new Donacion(List.of(unaCampera()), donante, "una donacion");
    donacion.confirmarDestino(beneficiaria);
    return donacion;
  }

  // El id de Donacion se autogenera en la BD; en este test unitario sin DB lo
  // forzamos por reflexion para que generarComprobante() no reviente al
  // desreferenciar donacion.id.
  private static void forzarId(Donacion donacion, long id) throws Exception {
    java.lang.reflect.Field campo = Donacion.class.getDeclaredField("id");
    campo.setAccessible(true);
    campo.set(donacion, id);
  }
}
