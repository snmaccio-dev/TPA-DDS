package donatrack.donaciones.service;

import donatrack.donaciones.domain.catalogo.Categoria;
import donatrack.donaciones.domain.catalogo.Subcategoria;
import donatrack.donaciones.domain.donacion.Bien;
import donatrack.donaciones.domain.donacion.CondicionBien;
import donatrack.donaciones.domain.donacion.Unidades;
import donatrack.donaciones.domain.persona.PersonaJuridica;
import donatrack.donaciones.domain.persona.TipoOrganizacion;
import org.junit.jupiter.api.Test;

import java.util.List;

public class GestorDonacionesTest {

  @Test
  public void ingresarDonacionSegmentaCorrectamentePorSubcategoria() {
    //GestorDonaciones gestor = new GestorDonaciones();
    //List<Donacion> resultado = gestor.ingresarDonacion(bienesVariados(), donanteArcosPlateados());

    //assertEquals(2, resultado.size());
  }

  private PersonaJuridica donanteArcosPlateados() {
    return new PersonaJuridica("Arcos Plateados S.A.", TipoOrganizacion.EMPRESA, "Mudanza");
  }

  private List<Bien> bienesVariados() {
    Subcategoria sillas = new Subcategoria("Sillas", new Categoria("Mobiliario"));
    Subcategoria fideos = new Subcategoria("Fideos", new Categoria("Alimentos"));

    return List.of(
        new Bien("Silla usada de oficina 1", sillas, 1,   Unidades.UNIDADES,   CondicionBien.USADO),
        new Bien("Silla usada de oficina 2", sillas, 1,   Unidades.UNIDADES,   CondicionBien.USADO),
        new Bien("Paquete fideos 500g",      fideos, 0.5, Unidades.KILOGRAMOS, CondicionBien.NUEVO)
    );
  }
}