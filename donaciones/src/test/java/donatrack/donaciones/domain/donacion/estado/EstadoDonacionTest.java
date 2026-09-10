package donatrack.donaciones.domain.donacion.estado;

import donatrack.donaciones.domain.catalogo.Categoria;
import donatrack.donaciones.domain.catalogo.Subcategoria;
import donatrack.donaciones.domain.donacion.Bien;
import donatrack.donaciones.domain.donacion.CambioEstado;
import donatrack.donaciones.domain.donacion.CondicionBien;
import donatrack.donaciones.domain.donacion.Donacion;
import donatrack.donaciones.domain.donacion.Unidades;
import donatrack.logistica.domain.flota.Camion;
import donatrack.donaciones.domain.persona.Beneficiaria;
import donatrack.donaciones.domain.persona.Donante;
import donatrack.donaciones.domain.persona.Genero;
import donatrack.donaciones.domain.persona.PersonaHumana;
import donatrack.donaciones.domain.persona.PersonaJuridica;
import donatrack.donaciones.domain.persona.TipoOrganizacion;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EstadoDonacionTest {

  @Test
  public void recorridoFelizAtraviesaLos7EstadosHastaEntregada() {
    Beneficiaria escuela = beneficiaria();
    Donacion donacion = donacionDeCampera();
    assertEquals("EN_DEPOSITO", donacion.getEstado().getNombre());

    donacion.confirmarDestino(escuela);
    assertEquals("ASIGNACION_REALIZADA", donacion.getEstado().getNombre());

    donacion.asignarCamion(unCamion());
    donacion.marcarListaParaEntregar();
    assertEquals("LISTA_PARA_ENTREGAR", donacion.getEstado().getNombre());

    donacion.marcarEnTraslado();
    assertEquals("EN_TRASLADO", donacion.getEstado().getNombre());

    donacion.confirmarRecepcion(List.of("foto1.jpg"));
    assertEquals("ENTREGADA", donacion.getEstado().getNombre());
    assertNotNull(donacion.getFechaHoraEntrega());
    assertEquals(List.of("foto1.jpg"), donacion.getFotos());
    assertTrue(escuela.getDonacionesRecibidas().contains(donacion));
  }

  @Test
  public void entregaFallidaPersisteElMotivoEnElHistorial() {
    Beneficiaria escuela = beneficiaria();
    Donacion donacion = donacionEnTraslado(escuela);

    donacion.marcarEntregaFallida("Nadie recibio");
    assertEquals("ENTREGA_FALLIDA", donacion.getEstado().getNombre());

    CambioEstado ultimo = donacion.getHistorialEstados().get(donacion.getHistorialEstados().size() - 1);
    assertEquals("Nadie recibio", ultimo.getMotivo());
  }

  @Test
  public void marcarEnDepositoLimpiaDestinatarioYCamion() {
    Beneficiaria escuela = beneficiaria();
    Donacion donacion = donacionEnTraslado(escuela);
    donacion.marcarEntregaFallida("Motivo");

    donacion.marcarEnDeposito();
    assertEquals("EN_DEPOSITO", donacion.getEstado().getNombre());
    assertNull(donacion.getDestinatarioAsignado());
    assertNull(donacion.getCamion());
  }

  @Test
  public void marcarEnDepositoSoloEsValidoTrasEntregaFallida() {
    Donacion donacion = donacionDeCampera();
    assertThrows(IllegalStateException.class, donacion::marcarEnDeposito);
  }

  @Test
  public void vencerEsValidoDesdeEnDeposito() {
    Donacion donacion = donacionDeCampera();
    donacion.vencer();
    assertEquals("VENCIDA", donacion.getEstado().getNombre());
  }

  @Test
  public void transicionInvalidaLanzaIllegalStateException() {
    Donacion donacion = donacionDeCampera();
    assertThrows(IllegalStateException.class, donacion::marcarEnTraslado);
  }

  @Test
  public void generarComprobanteRequiereEstarEntregada() {
    Donacion donacion = donacionDeCampera();
    assertThrows(IllegalStateException.class, donacion::generarComprobante);
  }

  // === helpers ===

  private Donacion donacionDeCampera() {
    Subcategoria ropa = new Subcategoria("Camperas de abrigo", new Categoria("Vestimenta"));
    Bien campera = new Bien("Campera talle M nueva", ropa, 1, Unidades.UNIDADES, CondicionBien.NUEVO);
    PersonaHumana persona = new PersonaHumana("Test", "Donante", 30, "0", Genero.MASCULINO);
    Donante donante = new Donante(persona);
    return new Donacion(List.of(campera), donante, "Campera nueva");
  }

  private Donacion donacionEnTraslado(Beneficiaria escuela) {
    Donacion donacion = donacionDeCampera();
    donacion.confirmarDestino(escuela);
    donacion.asignarCamion(unCamion());
    donacion.marcarListaParaEntregar();
    donacion.marcarEnTraslado();
    return donacion;
  }

  private Beneficiaria beneficiaria() {
    PersonaJuridica org = new PersonaJuridica("Escuela Test", TipoOrganizacion.INSTITUCION, "Educacion");
    return new Beneficiaria(org);
  }

  private Camion unCamion() {
    return new Camion("AAA111", 10, 3, 1000);
  }
}
