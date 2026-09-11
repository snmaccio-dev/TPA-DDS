package donatrack.logistica.domain.integracion;

import donatrack.logistica.contrato.DonacionParaPlanificar;
import donatrack.logistica.contrato.EventoEntrega;
import donatrack.logistica.contrato.EventoInicioRuta;
import donatrack.logistica.contrato.EventoRetornoDeposito;
import donatrack.logistica.contrato.EventoRutasPlanificadas;

import java.util.List;

public interface ClienteDonaciones {

  List<DonacionParaPlanificar> donacionesConAsignacionRealizada();

  void publicarRutasPlanificadas(EventoRutasPlanificadas evento);

  void publicarInicioRuta(EventoInicioRuta evento);

  void publicarEntrega(EventoEntrega evento);

  void publicarRetornoADeposito(EventoRetornoDeposito evento);
}
