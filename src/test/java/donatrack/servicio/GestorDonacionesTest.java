package donatrack.servicio;

import donatrack.model.catalogo.Categoria;
import donatrack.model.catalogo.Subcategoria;
import donatrack.model.donacion.Bien;
import donatrack.model.donacion.CondicionBien;
import donatrack.model.donacion.Unidades;
import donatrack.model.persona.PersonaJuridica;
import donatrack.model.persona.TipoOrganizacion;
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
        new Bien("Silla usada de oficina 1", sillas, Unidades.UNIDADES, CondicionBien.NUEVO),
        new Bien("Silla usada de oficina 2", sillas, Unidades.UNIDADES, CondicionBien.NUEVO),
        new Bien("Paquete fideos 500g", fideos, Unidades.KILOGRAMOS, CondicionBien.NUEVO)
    );
  }
}