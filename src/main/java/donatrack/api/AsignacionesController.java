package donatrack.api;

import donatrack.gestion.GestorAsignaciones;
import donatrack.model.donacion.Donacion;
import donatrack.model.persona.Beneficiaria;

import java.util.List;

public class AsignacionesController {

  private final GestorAsignaciones gestor =
      new GestorAsignaciones();

  public List<Beneficiaria> ejecutarAsignacion(
      Donacion donacion,
      List<Beneficiaria> beneficiarias) {

    return gestor.ejecutarAsignacion(
        donacion,
        beneficiarias
    );
  }
}
