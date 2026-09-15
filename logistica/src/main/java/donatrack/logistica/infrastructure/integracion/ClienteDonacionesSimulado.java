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
