package donatrack.servicio;

import donatrack.model.catalogo.Subcategoria;
import donatrack.model.donacion.Bien;
import donatrack.model.donacion.Donacion;
import donatrack.model.donacion.Unidades;
import donatrack.model.persona.PersonaJuridica;
import donatrack.model.persona.TipoOrganizacion;
import org.junit.jupiter.api.Test;
import servicio.ServicioDonaciones;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class ServicioDonacionesTest {

  @Test
  public void ingresarDonacionSegmentaCorrectamentePorSubcategoria() {
    ServicioDonaciones servicio = new ServicioDonaciones();
    List<Donacion> resultado = servicio.ingresarDonacion(bienesVariados(), donanteArcosPlateados());

    assertEquals(2, resultado.size());
  }

  private PersonaJuridica donanteArcosPlateados() {
    return new PersonaJuridica("Arcos Plateados S.A.", TipoOrganizacion.EMPRESA, "Mudanza");
  }

  private List<Bien> bienesVariados() {
    Subcategoria sillas = new Subcategoria("Sillas");
    Subcategoria fideos = new Subcategoria("Fideos");

    return List.of(
        new Bien("Silla usada de oficina 1", sillas, Unidades.UNIDADES),
        new Bien("Silla usada de oficina 2", sillas, Unidades.UNIDADES),
        new Bien("Paquete fideos 500g", fideos, Unidades.KILOGRAMOS)
    );
  }
}