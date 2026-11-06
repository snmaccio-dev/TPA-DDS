package donatrack.logistica.infrastructure.integracion;

import donatrack.logistica.contrato.DonacionParaPlanificar;
import donatrack.logistica.contrato.EventoEntrega;
import donatrack.logistica.contrato.EventoInicioRuta;
import donatrack.logistica.contrato.EventoRetornoDeposito;
import donatrack.logistica.contrato.EventoRutasPlanificadas;
import donatrack.logistica.domain.integracion.ClienteDonaciones;

import java.util.List;

public class ClienteDonacionesSimulado implements ClienteDonaciones {

  private final List<DonacionParaPlanificar> donaciones;

  public ClienteDonacionesSimulado() {
    this(List.of());
  }

  // Datos de ejemplo para poder ejercitar el flujo completo en modo simulado, donde no
  // hay un Donaciones del otro lado. Las beneficiarias y las personas donantes se cruzan
  // a proposito: ninguna de las dos dimensiones contiene a la otra, asi que sirven para
  // verificar que cada una ve solo lo suyo.
  public static ClienteDonacionesSimulado conDonacionesDeEjemplo() {
    return new ClienteDonacionesSimulado(List.of(
        new DonacionParaPlanificar(
            101L, "Alimentos no perecederos", 12, "cajas",
            900L, "Comedor Los Pinos", "Av. Hipolito Yrigoyen 1234, Lanus", 500L),
        new DonacionParaPlanificar(
            102L, "Ropa de abrigo", 8, "bolsas",
            900L, "Comedor Los Pinos", "Av. Hipolito Yrigoyen 1234, Lanus", 501L),
        new DonacionParaPlanificar(
            103L, "Productos de higiene", 20, "unidades",
            901L, "Centro La Esperanza", "Av. Meeks 820, Lomas de Zamora", 500L)
    ));
  }

  public ClienteDonacionesSimulado(List<DonacionParaPlanificar> donaciones) {
    this.donaciones = List.copyOf(donaciones);
  }

  @Override
  public List<DonacionParaPlanificar> donacionesConAsignacionRealizada() {
    System.out.println("[DONACIONES SIMULADO] GET /donaciones?estado=ASIGNACION_REALIZADA -> "
        + donaciones.size() + " donacion(es)");
    return donaciones;
  }

  @Override
  public void publicarRutasPlanificadas(EventoRutasPlanificadas evento) {
    System.out.println("[DONACIONES SIMULADO] POST /donaciones/eventos/rutas-planificadas"
        + " | ruta: " + evento.rutaId()
        + " | donaciones: " + evento.donacionIds());
  }

  @Override
  public void publicarInicioRuta(EventoInicioRuta evento) {
    System.out.println("[DONACIONES SIMULADO] POST /donaciones/eventos/inicio-ruta"
        + " | ruta: " + evento.rutaId()
        + " | donaciones: " + evento.donacionIds()
        + " | mapa: " + evento.linkMapa());
  }

  @Override
  public void publicarEntrega(EventoEntrega evento) {
    System.out.println("[DONACIONES SIMULADO] POST /donaciones/eventos/entrega"
        + " | donacion: " + evento.donacionId()
        + " | resultado: " + evento.resultado().getNombre()
        + " | camion: " + evento.patenteCamion()
        + (evento.motivo() == null ? "" : " | motivo: " + evento.motivo()));
  }

  @Override
  public void publicarRetornoADeposito(EventoRetornoDeposito evento) {
    System.out.println("[DONACIONES SIMULADO] POST /donaciones/eventos/retorno-deposito"
        + " | donacion: " + evento.donacionId());
  }
}
