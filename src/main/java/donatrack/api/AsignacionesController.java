package donatrack.api;

import donatrack.gestion.GestorAsignaciones;
import donatrack.model.donacion.Donacion;
import donatrack.model.entidad.EntidadBeneficiaria;

import java.util.List;

public class AsignacionesController {

  private final GestorAsignaciones gestor =
      new GestorAsignaciones();

  public List<EntidadBeneficiaria> ejecutarAsignacion(
      Donacion donacion,
      List<EntidadBeneficiaria> entidades) {

    return gestor.ejecutarAsignacion(
        donacion,
        entidades
    );
  }
}
